package com.englishapp.pronunciation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PhonemeMatch(
        int position,
        String expected,        // phoneme đúng
        String actual,          // phoneme user nói (null = bị bỏ sót)
        boolean matched,
        String tip              // gợi ý sửa lỗi, null nếu đúng
) {}
