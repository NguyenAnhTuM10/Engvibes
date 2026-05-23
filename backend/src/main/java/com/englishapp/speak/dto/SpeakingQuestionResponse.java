package com.englishapp.speak.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpeakingQuestionResponse {
    private String question;
    private List<String> suggestedVocab;
    private List<String> collocations;
}
