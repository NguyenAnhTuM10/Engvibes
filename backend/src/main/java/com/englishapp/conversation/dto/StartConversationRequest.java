package com.englishapp.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record StartConversationRequest(@NotBlank String scenarioId) {}
