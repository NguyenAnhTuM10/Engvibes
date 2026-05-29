package com.englishapp.pronunciation.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String targetText,
        String targetIpa,      // null cho đến lần attempt đầu tiên
        String sessionType,
        int attemptCount,
        Integer bestScore,     // điểm cao nhất trong các attempts
        Instant createdAt
) {}
