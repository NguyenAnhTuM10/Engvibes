package com.englishapp.conversation.dto;

import lombok.Builder;

@Builder
public record ConversationTurnResponse(
        int turnNumber,
        String userTranscript,
        String aiText,
        String aiAudioUrl,
        HintResponse hints,
        boolean isLastTurn
) {}
