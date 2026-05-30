package com.englishapp.pronunciation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WordAnalysis(
        String word,
        String heard,
        @JsonProperty("word_ipa") String wordIpa,
        int score
) {}
