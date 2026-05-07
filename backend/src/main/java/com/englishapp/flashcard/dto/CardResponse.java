package com.englishapp.flashcard.dto;

import com.englishapp.flashcard.CardSource;
import com.englishapp.flashcard.CardState;
import com.englishapp.vocab.dto.VocabResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private UUID id;
    private VocabResponse vocab;
    private UUID deckId;
    private CardState state;
    private double stability;
    private double difficulty;
    private Instant nextReview;
    private Instant lastReview;
    private int reviewCount;
    private int lapseCount;
    private String contextSentence;
    private UUID sourceVideoId;
    private CardSource sourceType;
}
