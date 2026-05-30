package com.englishapp.game.dto;

import java.util.UUID;

/** Typing recall: hiện nghĩa (back), yêu cầu gõ từ (front). */
public record TypingQuestion(UUID cardId, String back) {}
