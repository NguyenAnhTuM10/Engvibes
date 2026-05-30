package com.englishapp.sm2.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResult(
        UUID    cardId,
        int     quality,
        int     repetitions,
        int     intervalDays,
        double  easeFactor,
        Instant dueDate,
        Instant lastReviewed
) {}
