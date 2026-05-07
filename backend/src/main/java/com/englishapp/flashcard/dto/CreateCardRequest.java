package com.englishapp.flashcard.dto;

import com.englishapp.flashcard.CardSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {
    @NotNull
    private UUID vocabId;

    private UUID deckId;
    private String contextSentence;
    private UUID sourceVideoId;
    private UUID sourceSegmentId;
    private CardSource sourceType;
}
