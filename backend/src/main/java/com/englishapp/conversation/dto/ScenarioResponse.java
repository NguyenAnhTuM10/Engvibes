package com.englishapp.conversation.dto;

public record ScenarioResponse(
        String id,
        String displayName,
        String description,
        String aiRole,
        String userGoal,
        String openingLine
) {}
