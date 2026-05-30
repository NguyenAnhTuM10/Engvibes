package com.englishapp.sm2.dto;

import com.englishapp.sm2.Sm2Card;
import com.englishapp.sm2.Sm2Deck;

import java.util.List;

/** Deck "Sounds to practice" + cards (trả như deck thường). */
public record SoundsDeckResponse(Sm2Deck deck, List<Sm2Card> cards) {}
