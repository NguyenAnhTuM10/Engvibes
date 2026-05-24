package com.englishapp.conversation.dto;

import java.util.List;

public record ConversationReviewRequest(
        String scenarioId,
        List<Turn> turns,
        int durationSec
) {
    public record Turn(String role, String text) {}
}
