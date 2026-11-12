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
    /** Mặt trước / mặt sau / IPA đã được resolve — FE không cần phân biệt loại card. */
    private String front;
    private String back;
    private String ipa;
    /** true = card tự tạo (không gắn từ điển, {@code vocab} = null). */
    private boolean custom;
    /** Null với card tự tạo. */
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
