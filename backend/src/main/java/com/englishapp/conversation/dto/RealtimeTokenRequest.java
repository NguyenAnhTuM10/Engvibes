package com.englishapp.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record RealtimeTokenRequest(@NotBlank String scenarioId) {}
