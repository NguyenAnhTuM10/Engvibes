package com.englishapp.game.dto;

import java.util.List;
import java.util.UUID;

/** Câu hỏi multiple choice — KHÔNG chứa đáp án đúng. */
public record MultipleChoiceQuestion(
        UUID         cardId,
        String       front,          // từ cần đoán nghĩa
        List<String> options         // 4 lựa chọn đã xáo trộn, 1 đúng + 3 nhiễu
) {}
