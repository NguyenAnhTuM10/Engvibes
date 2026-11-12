package com.englishapp.flashcard;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.dto.ImportSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardImportServiceTest {

    @Mock DeckService deckService;
    @Mock CardRepository cardRepository;
    @Captor ArgumentCaptor<List<UserCard>> savedCards;

    CardImportService service;

    final UUID userId = UUID.randomUUID();
    final UUID deckId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CardImportService(deckService, cardRepository, new ObjectMapper());
    }

    @Test
    void importText_parsesLines_skipsDuplicatesAndReportsErrors() {
        // "learn" is already in the deck (e.g. as a dictionary card)
        given(cardRepository.findFrontsLowerInDeck(deckId)).willReturn(List.of("learn"));

        String content = String.join("\n",
                "think\tto have a thought",
                "speak",                                   // line 2: missing back
                "THINK\tduplicate within the batch",       // line 3: dup of line 1 (case-insensitive)
                "learn\tto acquire knowledge",             // line 4: already in deck
                "robust\tstrong\tróʊˈbʌst\tA robust design."
        );

        ImportSummary summary = service.importText(deckId, userId, content, "\\t", "\\n");

        assertThat(summary.imported()).isEqualTo(2);
        assertThat(summary.skipped()).isEqualTo(2);
        assertThat(summary.errors()).singleElement()
                .satisfies(e -> assertThat(e.line()).isEqualTo(2));

        verify(cardRepository).saveAll(savedCards.capture());
        List<UserCard> cards = savedCards.getValue();
        assertThat(cards).extracting(UserCard::getFront).containsExactly("think", "robust");
        assertThat(cards).allSatisfy(c -> {
            assertThat(c.getUserId()).isEqualTo(userId);
            assertThat(c.getDeckId()).isEqualTo(deckId);
            assertThat(c.isCustom()).isTrue();
            assertThat(c.getSourceType()).isEqualTo(CardSource.IMPORT);
            assertThat(c.getState()).isEqualTo(CardState.NEW);
            assertThat(c.getNextReview()).isNotNull();   // due immediately
        });
        UserCard robust = cards.get(1);
        assertThat(robust.getIpa()).isEqualTo("róʊˈbʌst");
        assertThat(robust.getContextSentence()).isEqualTo("A robust design.");
    }

    @Test
    void importCsv_stripsBomSkipsHeaderAndHandlesQuotedCommas() throws Exception {
        given(cardRepository.findFrontsLowerInDeck(deckId)).willReturn(List.of());
        String csv = "﻿front,back,ipa\n"
                   + "concise,\"brief, and clear\",kənˈsaɪs\n"
                   + "vague,not clear\n";

        ImportSummary summary = service.importCsv(deckId, userId,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(summary.imported()).isEqualTo(2);
        assertThat(summary.errors()).isEmpty();
        verify(cardRepository).saveAll(savedCards.capture());
        assertThat(savedCards.getValue()).extracting(UserCard::getBack)
                .containsExactly("brief, and clear", "not clear");
    }

    @Test
    void importJson_rejectsNonArrayPayload() {
        assertThatThrownBy(() -> service.importJson(deckId, userId,
                new ByteArrayInputStream("{\"front\":\"x\"}".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid JSON");
    }

    @Test
    void import_intoSomeoneElsesDeck_isRejectedBeforeAnythingIsSaved() {
        given(deckService.requireOwnedDeck(deckId, userId)).willThrow(ApiException.forbidden("Access denied"));

        assertThatThrownBy(() -> service.importText(deckId, userId, "a\tb", "\\t", "\\n"))
                .isInstanceOf(ApiException.class);
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    void import_overTheLimit_isRejected() {
        String content = "w\tmeaning\n".repeat(CardImportService.MAX_CARDS + 1);

        assertThatThrownBy(() -> service.importText(deckId, userId, content, "\\t", "\\n"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Import limit exceeded");
        verify(cardRepository, never()).saveAll(any());
    }
}
