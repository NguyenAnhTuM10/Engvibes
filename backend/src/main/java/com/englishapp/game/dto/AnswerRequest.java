package com.englishapp.game.dto;

import java.util.UUID;

public record AnswerRequest(UUID cardId, String selected) {}
