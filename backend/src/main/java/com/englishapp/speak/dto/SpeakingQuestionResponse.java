package com.englishapp.speak.dto;

import com.englishapp.ai.dto.WarmupWord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpeakingQuestionResponse {
    private String question;
    private List<WarmupWord> suggestedVocab;
    private List<String> suggestedCollocations;
}
