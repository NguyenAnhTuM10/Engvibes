package com.englishapp.game.dto;

import com.englishapp.flashcard.CardState;

import java.time.Instant;
import java.util.UUID;

public record TypingCheckResult(
        boolean   correct,
        String    correctAnswer,
        int       editDistance,   // Levenshtein distance giữa typed và correct
        // Trạng thái FSRS sau khi cập nhật
        UUID      cardId,
        CardState state,
        Instant   nextReview
) {}
