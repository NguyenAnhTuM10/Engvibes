package com.englishapp.pronunciation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Request gửi sang Python pronunciation-service
public record AnalyzeRequest(
        String transcript,
        @JsonProperty("target_text") String targetText,
        @JsonProperty("target_ipa")  String targetIpa
) {}
