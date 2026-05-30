package com.englishapp.game.dto;

import java.util.List;
import java.util.UUID;

public record MatchingCheckResult(
        int        correct,
        int        total,
        List<Wrong> wrongPairs
) {
    public record Wrong(UUID cardId, String front, String yourAnswer, String correctAnswer) {}
}
