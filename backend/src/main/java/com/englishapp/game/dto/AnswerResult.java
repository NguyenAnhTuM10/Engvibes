package com.englishapp.game.dto;

import com.englishapp.flashcard.CardState;

import java.time.Instant;
import java.util.UUID;

public record AnswerResult(
        boolean   correct,
        String    correctAnswer,
        // Trạng thái FSRS sau khi cập nhật
        UUID      cardId,
        CardState state,
        Instant   nextReview
) {}
