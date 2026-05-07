package com.englishapp.session.phrase.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhraseItem {
    private int idx;
    private String phrase;
    private String keyword;
}
