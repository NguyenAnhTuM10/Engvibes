package com.englishapp.video.subtitle;

import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final WhisperClient whisperClient;
    private final SubtitleRepository subtitleRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_WORDS_PER_SEGMENT = 15;

    @Transactional
    public List<SubtitleSegment> processVideoTranscription(UUID videoId, byte[] audioBytes) {
        WhisperResult result = whisperClient.transcribe(audioBytes, "audio.mp3");

        if (result == null || result.getWords() == null || result.getWords().isEmpty()) {
            log.warn("Whisper returned no words for video {}", videoId);
            return List.of();
        }

        subtitleRepository.deleteByVideoId(videoId);

        List<List<WhisperResult.WhisperWord>> groups = groupWords(result.getWords());
        List<SubtitleSegment> segments = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            List<WhisperResult.WhisperWord> group = groups.get(i);
            if (group.isEmpty()) continue;

            WhisperResult.WhisperWord first = group.get(0);
            WhisperResult.WhisperWord last = group.get(group.size() - 1);

            String segmentText = group.stream()
                    .map(WhisperResult.WhisperWord::getWord)
                    .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b)
                    .trim();

            String wordTimingsJson = toWordTimingsJson(group);

            SubtitleSegment segment = SubtitleSegment.builder()
                    .videoId(videoId)
                    .orderIndex(i)
                    .startMs((int) (first.getStart() * 1000))
                    .endMs((int) (last.getEnd() * 1000))
                    .text(segmentText)
                    .wordTimings(wordTimingsJson)
                    .build();

            segments.add(segment);
        }

        List<SubtitleSegment> saved = subtitleRepository.saveAll(segments);
        log.info("Saved {} subtitle segments for video {}", saved.size(), videoId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SubtitleSegment> getSegments(UUID videoId) {
        return subtitleRepository.findByVideoIdOrderByOrderIndex(videoId);
    }

    private List<List<WhisperResult.WhisperWord>> groupWords(List<WhisperResult.WhisperWord> words) {
        List<List<WhisperResult.WhisperWord>> groups = new ArrayList<>();
        List<WhisperResult.WhisperWord> current = new ArrayList<>();

        for (WhisperResult.WhisperWord word : words) {
            current.add(word);
            String w = word.getWord().trim();
            boolean endsSentence = w.endsWith(".") || w.endsWith("!") || w.endsWith("?");
            if (endsSentence || current.size() >= MAX_WORDS_PER_SEGMENT) {
                groups.add(new ArrayList<>(current));
                current.clear();
            }
        }
        if (!current.isEmpty()) groups.add(current);
        return groups;
    }

    private String toWordTimingsJson(List<WhisperResult.WhisperWord> words) {
        List<Map<String, Object>> timings = words.stream()
                .map(w -> Map.<String, Object>of(
                        "word", w.getWord().trim(),
                        "startMs", (int) (w.getStart() * 1000),
                        "endMs", (int) (w.getEnd() * 1000)))
                .toList();
        try {
            return objectMapper.writeValueAsString(timings);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
