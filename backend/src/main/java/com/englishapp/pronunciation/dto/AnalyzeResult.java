package com.englishapp.pronunciation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Response từ Python pronunciation-service
public record AnalyzeResult(
        @JsonProperty("target_ipa")       String targetIpa,
        @JsonProperty("actual_ipa")        String actualIpa,
        @JsonProperty("phoneme_matches")   List<RawPhonemeMatch> phonemeMatches,
        @JsonProperty("accuracy_score")    int accuracyScore,
        @JsonProperty("fluency_score")     int fluencyScore,
        @JsonProperty("overall_score")     int overallScore
) {
    // Inner record maps snake_case JSON fields từ Python
    public record RawPhonemeMatch(
            int position,
            String expected,
            String actual,
            boolean matched,
            String tip
    ) {}
}
