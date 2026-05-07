package com.englishapp.vocab;

import com.englishapp.user.CEFRLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VocabSeeder implements ApplicationRunner {

    private final VocabRepository vocabRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (vocabRepository.count() > 0) {
            return;
        }

        ClassPathResource resource = new ClassPathResource("seed/oxford_5000.csv");
        if (!resource.exists()) {
            log.warn("Vocab seed file not found at seed/oxford_5000.csv — skipping");
            return;
        }

        Map<String, VocabEntry> entryMap = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 || line.isBlank()) continue; // skip header

                String[] parts = line.split("\\|", -1);
                if (parts.length < 6) continue;

                try {
                    String word = parts[0].trim();
                    String pos = parts[2].trim();
                    entryMap.putIfAbsent(word + "|" + pos, VocabEntry.builder()
                            .word(word)
                            .cefrLevel(CEFRLevel.valueOf(parts[1].trim()))
                            .partOfSpeech(pos)
                            .ipa(parts[3].trim())
                            .phonemes(parts[4].trim())
                            .definition(parts[5].trim())
                            .build());
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid line {}: {}", lineNum, line);
                }
            }
        }

        List<VocabEntry> entries = new ArrayList<>(entryMap.values());
        vocabRepository.saveAll(entries);
        log.info("Seeded {} vocab entries", entries.size());
    }
}
