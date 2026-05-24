package com.englishapp.conversation.dto;

public record RealtimeTokenResponse(
        String token,
        String model,
        String scenarioId,
        String scenarioDisplayName,
        String aiRole,
        String userGoal,
        String openingLine
) {}
