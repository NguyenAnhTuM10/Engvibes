package com.englishapp.ai;

import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.user.CEFRLevel;
import com.englishapp.video.subtitle.SubtitleRepository;
import com.englishapp.video.subtitle.SubtitleSegment;
import com.englishapp.vocab.VocabEntry;
import com.englishapp.vocab.VocabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpService {

    private final SubtitleRepository subtitleRepository;
    private final VocabRepository vocabRepository;

    private static final List<CEFRLevel> CEFR_ORDER =
            List.of(CEFRLevel.A1, CEFRLevel.A2, CEFRLevel.B1, CEFRLevel.B2, CEFRLevel.C1, CEFRLevel.C2);

    // High-frequency function words to skip
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "this", "that", "these", "those",
            "and", "or", "but", "not", "for", "in", "on", "at", "to", "of",
            "with", "by", "from", "up", "about", "into", "through", "during",
            "it", "its", "he", "she", "they", "we", "you", "i", "me", "him",
            "her", "us", "them", "my", "your", "his", "our", "their", "what",
            "which", "who", "when", "where", "how", "why", "all", "each", "more",
            "also", "just", "than", "so", "if", "then", "as", "out", "get", "got",
            "like", "come", "know", "think", "see", "want", "look", "use", "make"
    );

    public List<WarmupWord> extractWarmupWords(UUID videoId, CEFRLevel videoLevel, int limit) {
        List<SubtitleSegment> segments = subtitleRepository.findByVideoIdOrderByOrderIndex(videoId);
        if (segments.isEmpty()) {
            log.warn("No subtitle segments for video {}", videoId);
            return List.of();
        }

        String fullText = segments.stream()
                .map(SubtitleSegment::getText)
                .collect(Collectors.joining(" "));

        Set<String> tokens = Arrays.stream(fullText.split("[^a-zA-Z]+"))
                .filter(w -> w.length() > 3)
                .map(String::toLowerCase)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());

        if (tokens.isEmpty()) return List.of();

        List<VocabEntry> found = vocabRepository.findByWordIn(new ArrayList<>(tokens));
        log.info("Video {} — {} tokens, {} matched in vocab", videoId, tokens.size(), found.size());

        return selectWarmupWords(found, videoLevel != null ? videoLevel : CEFRLevel.B1, limit);
    }

    private List<WarmupWord> selectWarmupWords(List<VocabEntry> candidates, CEFRLevel videoLevel, int limit) {
        int targetIdx = CEFR_ORDER.indexOf(videoLevel);

        return candidates.stream()
                .sorted(Comparator.comparingInt(v -> {
                    int levelIdx = CEFR_ORDER.indexOf(v.getCefrLevel());
                    int diff = Math.abs(levelIdx - targetIdx);
                    // Penalise words above video CEFR more than below
                    return levelIdx > targetIdx ? diff + 10 : diff;
                }))
                .limit(limit)
                .map(v -> new WarmupWord(v.getWord(), v.getIpa(), v.getDefinition(),
                        v.getCefrLevel(), v.getPartOfSpeech()))
                .toList();
    }
}
