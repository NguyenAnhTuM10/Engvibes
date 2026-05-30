package com.englishapp.sm2.dto;

import java.util.UUID;

public record CardRequest(
        UUID   deckId,
        String front,
        String back,
        String ipa,
        String exampleSentence
) {}
