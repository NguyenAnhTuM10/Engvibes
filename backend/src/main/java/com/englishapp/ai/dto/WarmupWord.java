package com.englishapp.ai.dto;

import com.englishapp.user.CEFRLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarmupWord(
        String word,
        String ipa,
        String definition,
        CEFRLevel cefrLevel,
        String partOfSpeech
) {}
