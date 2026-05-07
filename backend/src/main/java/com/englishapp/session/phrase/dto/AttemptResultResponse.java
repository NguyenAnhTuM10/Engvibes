package com.englishapp.session.phrase.dto;

import com.englishapp.shadow.WordMatch;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AttemptResultResponse {
    private UUID id;
    private String transcript;
    private List<WordMatch> wordMatches;
    private double accuracy;
}
