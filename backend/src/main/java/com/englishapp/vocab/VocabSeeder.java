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
import java.util.List;

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

        List<VocabEntry> entries = new ArrayList<>();
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
                    entries.add(VocabEntry.builder()
                            .word(parts[0].trim())
                            .cefrLevel(CEFRLevel.valueOf(parts[1].trim()))
                            .partOfSpeech(parts[2].trim())
                            .ipa(parts[3].trim())
                            .phonemes(parts[4].trim())
                            .definition(parts[5].trim())
                            .build());
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid line {}: {}", lineNum, line);
                }
            }
        }

        vocabRepository.saveAll(entries);
        log.info("Seeded {} vocab entries", entries.size());
    }
}
