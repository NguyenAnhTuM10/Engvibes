package com.englishapp.retell.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetellFeedback(
        @JsonProperty("coverage_score") int coverageScore,
        @JsonProperty("covered_points") List<CoveredPoint> coveredPoints,
        @JsonProperty("missed_points") List<String> missedPoints,
        @JsonProperty("vocab_score") int vocabScore,
        @JsonProperty("vocab_used") List<VocabUsed> vocabUsed,
        @JsonProperty("vocab_missed") List<String> vocabMissed,
        @JsonProperty("grammar_score") int grammarScore,
        @JsonProperty("grammar_issues") List<GrammarIssue> grammarIssues,
        @JsonProperty("positive_notes") List<String> positiveNotes,
        @JsonProperty("improvement_tips") List<String> improvementTips,
        @JsonProperty("model_answer") String modelAnswer,
        @JsonProperty("overall_score") int overallScore
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoveredPoint(
            String point,
            @JsonProperty("user_mention_quote") String userMentionQuote) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VocabUsed(
            String word,
            @JsonProperty("in_sentence") String inSentence) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GrammarIssue(
            @JsonProperty("error_quote") String errorQuote,
            String correction,
            @JsonProperty("brief_explain") String briefExplain) {}
}
