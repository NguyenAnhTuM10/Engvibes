package com.englishapp.game.dto;

import java.util.UUID;

public record TypingCheckRequest(UUID cardId, String typed) {}
