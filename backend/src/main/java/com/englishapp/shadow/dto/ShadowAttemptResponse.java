package com.englishapp.shadow.dto;

import com.englishapp.shadow.WordMatch;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ShadowAttemptResponse {
    private UUID id;
    private int attemptNumber;
    private String transcript;
    private List<WordMatch> wordMatches;
    private double accuracy;
    private List<String> weakPhonemes;
}
