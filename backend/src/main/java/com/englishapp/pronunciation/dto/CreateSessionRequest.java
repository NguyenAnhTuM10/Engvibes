package com.englishapp.pronunciation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank @Size(max = 200) String targetText,
        String targetIpa,      // optional — user có thể truyền sẵn để bỏ qua bước compute
        String sessionType     // "WORD" | "SENTENCE", default "WORD"
) {}
