package com.englishapp.conversation.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ConversationSessionResponse(
        UUID sessionId,
        String scenarioId,
        String scenarioDisplayName,
        String aiRole,
        String userGoal,
        String firstAiText,
        String firstAiAudioUrl,
        HintResponse hints
) {}
