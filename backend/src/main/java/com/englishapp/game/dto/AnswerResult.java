package com.englishapp.game.dto;

import java.time.Instant;
import java.util.UUID;

public record AnswerResult(
        boolean correct,
        String  correctAnswer,
        // SRS state sau khi cập nhật
        UUID    cardId,
        int     repetitions,
        int     intervalDays,
        double  easeFactor,
        Instant dueDate
) {}
