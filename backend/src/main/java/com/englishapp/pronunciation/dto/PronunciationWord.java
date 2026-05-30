package com.englishapp.pronunciation.dto;

/**
 * Một từ luyện phát âm trong nội dung tĩnh (data/pronunciation_content.json).
 *
 * @param text            từ cần phát âm
 * @param targetSound     âm trọng tâm (IPA, vd "θ") — dùng inventory của pronunciation-service
 * @param ipa             IPA đầy đủ của từ (vd "θɪŋk")
 * @param exampleSentence câu ví dụ chứa từ
 * @param group           nhóm âm (vd "Final Consonants")
 * @param vi              nghĩa tiếng Việt
 * @param commonError     lỗi người Việt hay mắc với từ/âm này
 * @param minimalPair     từ tương phản tối thiểu (vd "sink" cho "think") — có thể null
 * @param tip             gợi ý sửa lỗi ngắn (tiếng Việt)
 */
public record PronunciationWord(
        String text,
        String targetSound,
        String ipa,
        String exampleSentence,
        String group,
        String vi,
        String commonError,
        String minimalPair,
        String tip
) {}
