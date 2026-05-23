package com.englishapp.speak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HTTP response for freeform speaking practice (GPT Audio, IELTS 0-9 scale). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IeltsFeedbackResponse {
    private String transcript;
    private double fluency;
    private double grammar;
    private double vocabulary;
    private double pronunciation;
    private double overall;
    private String feedback;
}
