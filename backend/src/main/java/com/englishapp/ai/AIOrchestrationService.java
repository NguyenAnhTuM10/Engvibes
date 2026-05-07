package com.englishapp.ai;

import com.englishapp.ai.dto.VideoEnrichment;
import com.englishapp.ai.dto.VideoSummary;
import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.user.CEFRLevel;
import com.englishapp.video.subtitle.SubtitleRepository;
import com.englishapp.video.subtitle.SubtitleSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationService {

    private final LlmClient llmClient;
    private final NlpService nlpService;
    private final SubtitleRepository subtitleRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_COLLOCATIONS = "video:%s:collocations";
    private static final String CACHE_SUMMARY = "video:%s:summary";
    private static final int WARMUP_WORD_LIMIT = 5;

    private static final String COLLOCATION_SYSTEM =
            "You are an English language assistant. Return only valid JSON, no markdown code blocks.";
    private static final String SUMMARY_SYSTEM =
            "You are an English language learning assistant. Return only valid JSON, no markdown code blocks.";

    public VideoEnrichment enrichVideo(UUID videoId, CEFRLevel videoLevel) {
        List<WarmupWord> warmupWords = nlpService.extractWarmupWords(videoId, videoLevel, WARMUP_WORD_LIMIT);
        Map<String, List<String>> collocations = Map.of();

        if (!warmupWords.isEmpty()) {
            collocations = getCollocations(videoId, warmupWords);
        }

        return new VideoEnrichment(warmupWords, collocations);
    }

    public VideoSummary generateVideoSummary(UUID videoId) {
        String cacheKey = String.format(CACHE_SUMMARY, videoId);
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            try {
                return objectMapper.readValue(cached, VideoSummary.class);
            } catch (Exception e) {
                log.warn("Stale summary cache for video {}, regenerating", videoId);
            }
        }

        String transcript = buildTranscript(videoId);
        if (transcript.isBlank()) {
            log.warn("Empty transcript for video {}, skipping summary", videoId);
            return VideoSummary.empty();
        }

        try {
            String userPrompt = """
                    Analyze this English video transcript for language learners and return a JSON object with exactly these fields:
                    - "summary": a 2-3 sentence plain English summary
                    - "keyPoints": an array of 3-5 key learning points as strings
                    - "speakingQuestion": one open-ended speaking practice question based on the content

                    Transcript:
                    """ + transcript;

            String raw = llmClient.chatCompletion(SUMMARY_SYSTEM, userPrompt);
            VideoSummary summary = objectMapper.readValue(extractJson(raw), VideoSummary.class);

            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(summary));
            log.info("Generated and cached summary for video {}", videoId);
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for video {}: {}", videoId, e.getMessage());
            return VideoSummary.empty();
        }
    }

    private Map<String, List<String>> getCollocations(UUID videoId, List<WarmupWord> warmupWords) {
        String cacheKey = String.format(CACHE_COLLOCATIONS, videoId);
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Stale collocations cache for video {}, regenerating", videoId);
            }
        }

        try {
            String wordList = warmupWords.stream()
                    .map(WarmupWord::word)
                    .collect(Collectors.joining(", "));

            String userPrompt = String.format("""
                    For each of these English words, provide exactly 3 common collocations \
                    (natural word combinations used in everyday English).
                    Return as a JSON object where each key is a word and the value is an array of 3 collocation strings.

                    Words: %s
                    """, wordList);

            String raw = llmClient.chatCompletion(COLLOCATION_SYSTEM, userPrompt);
            Map<String, List<String>> collocations = objectMapper.readValue(
                    extractJson(raw), new TypeReference<>() {});

            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(collocations));
            log.info("Generated and cached collocations for video {} ({} words)", videoId, warmupWords.size());
            return collocations;

        } catch (Exception e) {
            log.error("Failed to generate collocations for video {}: {}", videoId, e.getMessage());
            return Map.of();
        }
    }

    private String buildTranscript(UUID videoId) {
        return subtitleRepository.findByVideoIdOrderByOrderIndex(videoId).stream()
                .map(SubtitleSegment::getText)
                .collect(Collectors.joining(" "));
    }

    // Strip markdown code fences that some LLMs include despite instructions
    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
