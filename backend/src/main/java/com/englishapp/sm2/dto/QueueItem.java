package com.englishapp.sm2.dto;

import java.time.Instant;
import java.util.UUID;

/** 1 card trong review queue, kèm trạng thái SRS hiện tại (null nếu card mới). */
public record QueueItem(
        UUID    cardId,
        UUID    deckId,
        String  front,
        String  back,
        String  ipa,
        String  exampleSentence,
        boolean isNew,           // true = chưa có review record
        // SRS state (null khi isNew=true)
        Integer repetitions,
        Integer intervalDays,
        Double  easeFactor,
        Instant dueDate,
        Instant lastReviewed
) {}
