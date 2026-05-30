package com.englishapp.pronunciation.dto;

/**
 * Một từ luyện phát âm trong nội dung tĩnh (data/pronunciation_content.json).
 *
 * @param text            từ cần phát âm
 * @param targetSound     âm trọng tâm (IPA, vd "θ") — dùng inventory của pronunciation-service
 * @param ipa             IPA đầy đủ của từ (vd "θɪŋk")
 * @param exampleSentence câu ví dụ chứa từ
 * @param group           nhóm âm (vd "Final Consonants")
 */
public record PronunciationWord(
        String text,
        String targetSound,
        String ipa,
        String exampleSentence,
        String group
) {}
