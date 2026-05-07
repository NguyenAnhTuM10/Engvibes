package com.englishapp.vocab.dto;

import com.englishapp.user.CEFRLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabResponse {
    private UUID id;
    private String word;
    private String partOfSpeech;
    private CEFRLevel cefrLevel;
    private String ipa;
    private String phonemes;
    private String definition;
}
