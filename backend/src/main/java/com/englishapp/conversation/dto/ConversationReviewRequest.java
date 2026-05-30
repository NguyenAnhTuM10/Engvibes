package com.englishapp.conversation.dto;

import java.util.List;

/**
 * T2.2 — sessionId là nguồn transcript chính thức (server-observed).
 * turns/scenarioId từ client giữ lại để backward-compat nhưng KHÔNG dùng để chấm
 * khi sessionId có mặt — chống client chế transcript gian điểm.
 */
public record ConversationReviewRequest(
        java.util.UUID sessionId,
        String scenarioId,
        List<Turn> turns,
        int durationSec
) {
    public record Turn(String role, String text) {}
}
