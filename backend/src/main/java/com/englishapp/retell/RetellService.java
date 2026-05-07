package com.englishapp.retell;

import com.englishapp.ai.LlmClient;
import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.common.ApiException;
import com.englishapp.retell.dto.*;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.storage.StorageService;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RetellService {

    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final RetellAttemptRepository retellAttemptRepository;
    private final WhisperClient whisperClient;
    private final LlmClient llmClient;
    private final StorageService storageService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    private static final int DAILY_RETELL_LIMIT = 50;
    private static final List<String> SENTENCE_STARTERS = List.of(
            "The video is about...",
            "First, the speaker mentions...",
            "One key point is that...",
            "In addition, ...",
            "In the end, ..."
    );

    private static final String SYSTEM_PROMPT =
            "You are a supportive English speaking coach. Return only valid JSON, no markdown code blocks.";

    public RetellScaffoldResponse startRetell(UUID sessionId, UUID userId, StartRetellRequest request) {
        LearningSession session = requireOwned(sessionId, userId);
        session.setScaffoldLevel(request.getScaffoldLevel());
        sessionRepository.save(session);

        Video video = loadVideo(session.getVideoId());
        int level = request.getScaffoldLevel();

        return switch (level) {
            case 2 -> RetellScaffoldResponse.builder()
                    .level(2)
                    .wordBank(parseWarmupWords(video.getWarmupWordsJson()))
                    .build();
            case 3 -> RetellScaffoldResponse.builder()
                    .level(3)
                    .sentenceStarters(SENTENCE_STARTERS)
                    .build();
            case 4 -> RetellScaffoldResponse.builder()
                    .level(4)
                    .storyFrame(buildStoryFrame(video.getSummary()))
                    .build();
            default -> RetellScaffoldResponse.builder().level(1).build();
        };
    }

    public RetellAttemptSummary submitAttempt(UUID sessionId, UUID userId, MultipartFile audio) {
        if (!rateLimitService.tryAcquire(userId, "retell", DAILY_RETELL_LIMIT)) {
            throw ApiException.badRequest("Daily retell limit reached (50/day). Try again tomorrow.");
        }

        LearningSession session = requireOwned(sessionId, userId);
        Video video = loadVideo(session.getVideoId());
        int scaffoldLevel = session.getScaffoldLevel() != null ? session.getScaffoldLevel() : 1;
        int attemptNumber = retellAttemptRepository.countBySessionId(sessionId) + 1;

        String audioKey = String.format("sessions/%s/retell-%d.webm", sessionId, attemptNumber);
        try {
            byte[] bytes = audio.getBytes();
            storageService.upload(recordingsBucket, audioKey,
                    new ByteArrayInputStream(bytes), bytes.length,
                    audio.getContentType() != null ? audio.getContentType() : "audio/webm");

            WhisperResult whisperResult = whisperClient.transcribe(bytes,
                    audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm",
                    audio.getContentType());

            String transcript = whisperResult != null && whisperResult.getText() != null
                    ? whisperResult.getText().trim() : "";
            int durationSec = estimateDuration(whisperResult);

            String prompt = buildRetellPrompt(video, transcript, scaffoldLevel, durationSec, userId);
            String raw = llmClient.chatCompletion(SYSTEM_PROMPT, prompt);
            RetellFeedback feedback = objectMapper.readValue(extractJson(raw), RetellFeedback.class);

            RetellAttempt attempt = RetellAttempt.builder()
                    .sessionId(sessionId)
                    .attemptNumber(attemptNumber)
                    .audioUrl(audioKey)
                    .transcript(transcript)
                    .scaffoldLevel(scaffoldLevel)
                    .aiFeedback(objectMapper.writeValueAsString(feedback))
                    .overallScore(feedback.overallScore())
                    .durationSec(durationSec)
                    .build();
            RetellAttempt saved = retellAttemptRepository.save(attempt);

            log.info("Retell attempt {} for session {} — score={}", attemptNumber, sessionId, feedback.overallScore());
            return toSummary(saved, feedback);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Retell attempt failed for session {}: {}", sessionId, e.getMessage());
            throw ApiException.badRequest("Failed to process retell: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<RetellAttemptSummary> getAttempts(UUID sessionId, UUID userId) {
        requireOwned(sessionId, userId);
        return retellAttemptRepository.findBySessionIdOrderByCreatedAt(sessionId).stream()
                .map(a -> toSummary(a, parseFeedback(a.getAiFeedback())))
                .toList();
    }

    private String buildRetellPrompt(Video video, String transcript, int scaffoldLevel, int durationSec, UUID userId) {
        List<String> keyPoints = parseKeyPoints(video.getKeyPointsJson());
        String keyPointsNumbered = IntStream.range(0, keyPoints.size())
                .mapToObj(i -> (i + 1) + ". " + keyPoints.get(i))
                .collect(Collectors.joining("\n"));

        List<WarmupWord> warmup = parseWarmupWords(video.getWarmupWordsJson());
        String vocabList = warmup.stream().map(WarmupWord::word).collect(Collectors.joining(", "));

        Map<String, List<String>> collocations = parseCollocations(video.getCollocationsJson());
        String collocationsList = collocations.values().stream()
                .flatMap(List::stream).limit(10).collect(Collectors.joining(", "));

        return """
                You are a supportive English speaking coach evaluating how well a B1 learner retold a video they just watched.

                VIDEO CONTEXT:
                - Short summary: %s
                - Key points (these are the ground truth the user should cover):
                %s
                - Important vocabulary from video: %s
                - Important collocations: %s

                USER'S RETELLING:
                - Transcript: "%s"
                - Duration: %ds
                - Scaffold level used: %d (1=no help, 4=full template)

                Return a JSON object (no markdown) with this structure:
                {
                  "coverage_score": <int 0-100>,
                  "covered_points": [{"point": "<key point>", "user_mention_quote": "<quote from user>"}],
                  "missed_points": ["<key point>"],
                  "vocab_score": <int 0-100>,
                  "vocab_used": [{"word": "<word>", "in_sentence": "<user sentence>"}],
                  "vocab_missed": ["<vocab from video not used>"],
                  "grammar_score": <int 0-100>,
                  "grammar_issues": [{"error_quote": "<wrong text>", "correction": "<fixed>", "brief_explain": "<why>"}],
                  "positive_notes": ["<specific good thing>"],
                  "improvement_tips": ["<focused actionable tip>"],
                  "model_answer": "<B1 model answer covering all key points in 2-3 sentences>",
                  "overall_score": <int 0-100>
                }

                RULES: Be encouraging. Max 3 grammar issues. If scaffold_level >= 3, don't penalize for using scaffold.
                Coverage = how many key points addressed. Overall = 0.5*coverage + 0.25*vocab + 0.25*grammar.
                """.formatted(
                video.getSummary() != null ? video.getSummary() : "Not available",
                keyPointsNumbered.isEmpty() ? "No key points available" : keyPointsNumbered,
                vocabList.isEmpty() ? "None" : vocabList,
                collocationsList.isEmpty() ? "None" : collocationsList,
                transcript.isEmpty() ? "(no speech detected)" : transcript,
                durationSec,
                scaffoldLevel
        );
    }

    private String buildStoryFrame(String summary) {
        if (summary != null && !summary.isBlank()) {
            return "The video is about ___. The main idea is ___. One thing that stood out was ___. Overall, I think ___.";
        }
        return "The video is about ___. The main idea is ___. One thing that stood out was ___. Overall, I think ___.";
    }

    private int estimateDuration(WhisperResult result) {
        if (result == null) return 0;
        if (result.getWords() != null && !result.getWords().isEmpty()) {
            return (int) result.getWords().get(result.getWords().size() - 1).getEnd();
        }
        if (result.getSegments() != null && !result.getSegments().isEmpty()) {
            return (int) result.getSegments().get(result.getSegments().size() - 1).getEnd();
        }
        return 0;
    }

    private RetellAttemptSummary toSummary(RetellAttempt attempt, RetellFeedback feedback) {
        return RetellAttemptSummary.builder()
                .id(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .scaffoldLevel(attempt.getScaffoldLevel())
                .overallScore(attempt.getOverallScore())
                .durationSec(attempt.getDurationSec())
                .transcript(attempt.getTranscript())
                .feedback(feedback)
                .createdAt(attempt.getCreatedAt())
                .build();
    }

    private RetellFeedback parseFeedback(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, RetellFeedback.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<WarmupWord> parseWarmupWords(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<WarmupWord>>() {});
        } catch (Exception e) { return List.of(); }
    }

    private List<String> parseKeyPoints(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) { return List.of(); }
    }

    private Map<String, List<String>> parseCollocations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) { return Map.of(); }
    }

    private LearningSession requireOwned(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return session;
    }

    private Video loadVideo(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) return trimmed.substring(start, end).trim();
        }
        return trimmed;
    }
}
