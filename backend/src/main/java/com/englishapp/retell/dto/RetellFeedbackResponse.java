package com.englishapp.retell.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetellFeedbackResponse {
    private int score;
    private int coverageScore;
    private int vocabularyScore;
    private int grammarScore;
    private String transcript;
    private List<String> coveredPoints;
    private List<String> missedPoints;
    private List<String> usedVocab;
    private List<String> missedVocab;
    private List<GrammarIssue> grammarIssues;
    private List<String> positiveNotes;
    private List<String> improvementTips;
    private String modelAnswer;

    public record GrammarIssue(String errorQuote, String correction, String explanation) {}
}
