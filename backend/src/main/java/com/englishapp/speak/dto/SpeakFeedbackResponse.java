package com.englishapp.speak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakFeedbackResponse {
    private int score;
    private int fluencyScore;
    private int grammarScore;
    private int vocabVarietyScore;
    private String transcript;
    private List<String> vocabFromVideoUsed;
    private List<GrammarIssue> grammarIssues;
    private List<String> positiveNotes;
    private List<String> improvementTips;
    private String modelAnswer;

    public record GrammarIssue(String errorQuote, String correction, String explanation) {}
}
