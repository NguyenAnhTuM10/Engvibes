package com.englishapp.game.dto;

import java.time.Instant;
import java.util.UUID;

public record TypingCheckResult(
        boolean correct,
        String  correctAnswer,
        int     editDistance,   // Levenshtein distance giữa typed và correct
        UUID    cardId,
        int     repetitions,
        int     intervalDays,
        double  easeFactor,
        Instant dueDate
) {}
