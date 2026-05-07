package com.englishapp.shadow;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CmuDictService {

    private Map<String, List<String>> dict = new HashMap<>();

    @PostConstruct
    public void load() {
        ClassPathResource resource = new ClassPathResource("data/cmudict.txt");
        if (!resource.exists()) {
            log.warn("cmudict.txt not found at classpath:data/cmudict.txt — phoneme detection disabled. " +
                    "Download from http://www.speech.cs.cmu.edu/cgi-bin/cmudict and place at " +
                    "src/main/resources/data/cmudict.txt");
            return;
        }
        long start = System.currentTimeMillis();
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";;;")) continue;
                // Skip alternate pronunciations like WORD(2)
                if (line.contains("(")) continue;
                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) continue;
                String word = parts[0].toLowerCase();
                List<String> phonemes = List.of(parts[1].split("\\s+")).stream()
                        .map(p -> p.replaceAll("[012]$", ""))
                        .toList();
                dict.put(word, phonemes);
                count++;
            }
        } catch (Exception e) {
            log.error("Failed to load CMU dict: {}", e.getMessage());
        }
        log.info("Loaded {} CMU dict entries in {}ms", count, System.currentTimeMillis() - start);
    }

    public List<String> getPhonemes(String word) {
        return dict.getOrDefault(word.toLowerCase(), List.of());
    }

    public boolean isLoaded() {
        return !dict.isEmpty();
    }
}
