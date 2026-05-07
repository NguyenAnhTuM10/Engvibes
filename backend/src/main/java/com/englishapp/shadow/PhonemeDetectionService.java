package com.englishapp.shadow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhonemeDetectionService {

    private final CmuDictService cmuDictService;
    private final UserPhonemeStatsRepository phonemeStatsRepository;

    public List<String> detectWeakPhonemes(List<WordMatch> wordMatches) {
        if (!cmuDictService.isLoaded()) return List.of();
        Set<String> weak = new LinkedHashSet<>();
        for (WordMatch match : wordMatches) {
            if (match.status() == WordMatch.Status.MISSING
                    || match.status() == WordMatch.Status.MISPRONOUNCED) {
                if (match.expected() != null) {
                    weak.addAll(cmuDictService.getPhonemes(match.expected()));
                }
            }
        }
        return List.copyOf(weak);
    }

    @Transactional
    public void updateUserPhonemeStats(UUID userId, List<WordMatch> wordMatches) {
        if (!cmuDictService.isLoaded()) return;

        // All phonemes the user attempted (from all spoken/expected words)
        Map<String, Integer> totalCount = new HashMap<>();
        Map<String, Integer> errorCount = new HashMap<>();

        for (WordMatch match : wordMatches) {
            String sourceWord = match.status() == WordMatch.Status.EXTRA
                    ? match.actual()
                    : match.expected();
            if (sourceWord == null) continue;

            List<String> phonemes = cmuDictService.getPhonemes(sourceWord);
            for (String phoneme : phonemes) {
                totalCount.merge(phoneme, 1, Integer::sum);
                if (match.status() == WordMatch.Status.MISSING
                        || match.status() == WordMatch.Status.MISPRONOUNCED) {
                    errorCount.merge(phoneme, 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : totalCount.entrySet()) {
            String phoneme = entry.getKey();
            int total = entry.getValue();
            int errors = errorCount.getOrDefault(phoneme, 0);
            phonemeStatsRepository.upsert(userId, phoneme, total, errors);
        }
    }
}
