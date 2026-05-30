package com.englishapp.pronunciation;

import com.englishapp.pronunciation.dto.PronunciationSentence;
import com.englishapp.pronunciation.dto.PronunciationWord;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Nội dung tĩnh cho trang Pronunciation Practice (words + sentences).
 *
 * Load 1 lần lúc startup từ classpath `data/pronunciation_content.json`.
 * KHÔNG gọi LLM lúc runtime — toàn bộ IPA + câu ví dụ đã soạn sẵn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationContentService {

    private static final String CONTENT_PATH = "data/pronunciation_content.json";

    private final ObjectMapper objectMapper;

    private List<PronunciationWord> words = List.of();
    private List<PronunciationSentence> sentences = List.of();

    private record Content(List<PronunciationWord> words, List<PronunciationSentence> sentences) {}

    @PostConstruct
    void load() {
        ClassPathResource resource = new ClassPathResource(CONTENT_PATH);
        if (!resource.exists()) {
            log.warn("Pronunciation content not found at {} — words/sentences will be empty", CONTENT_PATH);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            Content content = objectMapper.readValue(in, Content.class);
            this.words = content.words() != null ? content.words() : List.of();
            this.sentences = content.sentences() != null ? content.sentences() : List.of();
            log.info("Loaded pronunciation content: {} words, {} sentences",
                    words.size(), sentences.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + CONTENT_PATH, e);
        }
    }

    /** Tất cả words, hoặc lọc theo group nếu truyền vào. Giữ nguyên thứ tự trong file. */
    public List<PronunciationWord> getWords(String group) {
        if (group == null || group.isBlank()) {
            return words;
        }
        return words.stream().filter(w -> group.equalsIgnoreCase(w.group())).toList();
    }

    /** Tất cả sentences, hoặc lọc theo category nếu truyền vào. Giữ nguyên thứ tự trong file. */
    public List<PronunciationSentence> getSentences(String category) {
        if (category == null || category.isBlank()) {
            return sentences;
        }
        return sentences.stream().filter(s -> category.equalsIgnoreCase(s.category())).toList();
    }

    /** Danh sách group (theo thứ tự xuất hiện) — tiện cho FE render tab. */
    public List<String> getWordGroups() {
        return words.stream().map(PronunciationWord::group).distinct().toList();
    }

    /** Danh sách category (theo thứ tự xuất hiện). */
    public List<String> getSentenceCategories() {
        return sentences.stream().map(PronunciationSentence::category).distinct().toList();
    }

    /** Tra 1 từ trong content (case-insensitive) — dùng để làm giàu card SRS (ipa + ví dụ). */
    public java.util.Optional<PronunciationWord> findWord(String text) {
        if (text == null) return java.util.Optional.empty();
        String t = text.trim();
        return words.stream().filter(w -> w.text().equalsIgnoreCase(t)).findFirst();
    }
}
