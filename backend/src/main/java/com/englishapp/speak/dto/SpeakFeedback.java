package com.englishapp.speak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Internal DTO for parsing LLM JSON in session-based speak (Whisper + text eval). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeakFeedback(
        @JsonProperty("fluency_score")        int fluencyScore,
        @JsonProperty("grammar_score")        int grammarScore,
        @JsonProperty("vocab_variety_score")  int vocabVarietyScore,
        @JsonProperty("vocab_from_video_used") List<VocabUsed> vocabFromVideoUsed,
        @JsonProperty("vocab_from_video_missed") List<String> vocabFromVideoMissed,
        @JsonProperty("grammar_issues")       List<GrammarIssue> grammarIssues,
        @JsonProperty("positive_notes")       List<String> positiveNotes,
        @JsonProperty("improvement_tips")     List<String> improvementTips,
        @JsonProperty("model_answer")         String modelAnswer,
        @JsonProperty("overall_score")        int overallScore
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VocabUsed(String word, @JsonProperty("in_sentence") String inSentence) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GrammarIssue(
            @JsonProperty("error_quote")   String errorQuote,
            String correction,
            @JsonProperty("brief_explain") String briefExplain) {}
}
