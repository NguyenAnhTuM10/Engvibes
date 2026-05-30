package com.englishapp.pronunciation.dto;

/**
 * Một câu luyện phát âm trong nội dung tĩnh (data/pronunciation_content.json).
 *
 * @param text        câu cần phát âm
 * @param level       CEFR level (B1/B2/C1)
 * @param targetSound âm trọng tâm của drill (IPA) — null nếu câu không nhắm 1 âm cụ thể
 * @param category    nhóm câu (vd "Tongue Twisters")
 * @param vi          bản dịch tiếng Việt
 * @param tip         ghi chú trọng tâm luyện của câu (tiếng Việt)
 */
public record PronunciationSentence(
        String text,
        String level,
        String targetSound,
        String category,
        String vi,
        String tip
) {}
