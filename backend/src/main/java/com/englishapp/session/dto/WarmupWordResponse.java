package com.englishapp.session.dto;

import com.englishapp.user.CEFRLevel;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WarmupWordResponse {
    private UUID vocabId;
    private String word;
    private String ipa;
    private String definition;
    private CEFRLevel cefrLevel;
    private String partOfSpeech;
    private int priorityOrder;
}
