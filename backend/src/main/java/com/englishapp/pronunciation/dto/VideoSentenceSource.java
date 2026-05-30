package com.englishapp.pronunciation.dto;

import java.util.UUID;

/**
 * Một video (đã PUBLISHED và CÓ phụ đề) làm nguồn câu luyện phát âm.
 * Dùng cho picker "From Video" trong trang Pronunciation.
 *
 * @param sentenceCount số câu phụ đề có thể luyện
 */
public record VideoSentenceSource(
        UUID id,
        String title,
        String cefrLevel,
        int sentenceCount
) {}
