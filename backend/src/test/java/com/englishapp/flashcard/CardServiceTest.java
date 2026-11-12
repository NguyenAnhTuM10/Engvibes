package com.englishapp.flashcard;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.flashcard.dto.CreateCardRequest;
import com.englishapp.vocab.VocabEntry;
import com.englishapp.vocab.VocabRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock CardRepository cardRepository;
    @Mock DeckService deckService;
    @Mock VocabRepository vocabRepository;
    @Mock FsrsScheduler fsrsScheduler;
    @Mock FlashcardMapper flashcardMapper;

    @InjectMocks CardService cardService;

    final UUID userId = UUID.randomUUID();
    final UUID deckId = UUID.randomUUID();

    private FlashcardDeck ownedDeck() {
        return FlashcardDeck.builder().id(deckId).userId(userId).name("Deck").build();
    }

    @Test
    void addCard_withFrontAndBack_createsCustomCardDueNow() {
        given(deckService.requireOwnedDeck(deckId, userId)).willReturn(ownedDeck());
        given(cardRepository.findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(deckId, "resilient"))
                .willReturn(Optional.empty());
        given(cardRepository.save(any(UserCard.class))).willAnswer(inv -> inv.getArgument(0));
        given(flashcardMapper.toCardResponse(any())).willReturn(new CardResponse());

        CreateCardRequest req = new CreateCardRequest();
        req.setDeckId(deckId);
        req.setFront("  resilient ");
        req.setBack("able to recover quickly");
        req.setIpa(" ");

        cardService.addCard(req, userId);

        ArgumentCaptor<UserCard> saved = ArgumentCaptor.forClass(UserCard.class);
        verify(cardRepository).save(saved.capture());
        UserCard card = saved.getValue();
        assertThat(card.isCustom()).isTrue();
        assertThat(card.resolvedFront()).isEqualTo("resilient");
        assertThat(card.resolvedBack()).isEqualTo("able to recover quickly");
        assertThat(card.resolvedIpa()).isNull();
        assertThat(card.getSourceType()).isEqualTo(CardSource.MANUAL);
        assertThat(card.getNextReview()).isNotNull();
        verify(vocabRepository, never()).findById(any());
    }

    @Test
    void addCard_customDuplicateInSameDeck_returnsExistingCard() {
        UserCard existing = UserCard.builder().id(UUID.randomUUID()).userId(userId).deckId(deckId)
                .front("resilient").back("old meaning").build();
        given(deckService.requireOwnedDeck(deckId, userId)).willReturn(ownedDeck());
        given(cardRepository.findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(deckId, "Resilient"))
                .willReturn(Optional.of(existing));

        CreateCardRequest req = new CreateCardRequest();
        req.setDeckId(deckId);
        req.setFront("Resilient");
        req.setBack("new meaning");

        cardService.addCard(req, userId);

        verify(cardRepository, never()).save(any());
        verify(flashcardMapper).toCardResponse(existing);
    }

    @Test
    void addCard_intoSomeoneElsesDeck_isForbidden() {
        given(deckService.requireOwnedDeck(deckId, userId)).willThrow(ApiException.forbidden("Access denied"));

        CreateCardRequest req = new CreateCardRequest();
        req.setDeckId(deckId);
        req.setFront("x");
        req.setBack("y");

        assertThatThrownBy(() -> cardService.addCard(req, userId)).isInstanceOf(ApiException.class);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void getDueCards_ofSomeoneElsesDeck_isForbidden() {
        given(deckService.requireOwnedDeck(deckId, userId)).willThrow(ApiException.forbidden("Access denied"));

        assertThatThrownBy(() -> cardService.getDueCards(deckId, 20, userId)).isInstanceOf(ApiException.class);
    }

    @Test
    void reviewCard_ofAnotherUser_isForbidden() {
        UUID cardId = UUID.randomUUID();
        UserCard othersCard = UserCard.builder().id(cardId).userId(UUID.randomUUID()).deckId(deckId)
                .front("a").back("b").build();
        given(cardRepository.findById(cardId)).willReturn(Optional.of(othersCard));

        assertThatThrownBy(() -> cardService.reviewCard(cardId, 3, userId)).isInstanceOf(ApiException.class);
        verify(fsrsScheduler, never()).review(any(), anyInt());
    }

    @Test
    void createCardRequest_requiresVocabIdOrFrontAndBack() {
        CreateCardRequest empty = new CreateCardRequest();
        assertThat(empty.isContentValid()).isFalse();

        CreateCardRequest frontOnly = new CreateCardRequest();
        frontOnly.setFront("word");
        assertThat(frontOnly.isContentValid()).isFalse();

        CreateCardRequest custom = new CreateCardRequest();
        custom.setFront("word");
        custom.setBack("meaning");
        assertThat(custom.isContentValid()).isTrue();

        CreateCardRequest dictionary = new CreateCardRequest();
        dictionary.setVocabId(UUID.randomUUID());
        assertThat(dictionary.isContentValid()).isTrue();
    }

    @Test
    void userCard_resolvesContentFromVocabForDictionaryCards() {
        VocabEntry vocab = VocabEntry.builder().word("achieve").definition("to succeed").ipa("əˈtʃiːv").build();
        UserCard card = UserCard.builder().userId(userId).deckId(deckId).vocab(vocab).build();

        assertThat(card.isCustom()).isFalse();
        assertThat(card.resolvedFront()).isEqualTo("achieve");
        assertThat(card.resolvedBack()).isEqualTo("to succeed");
        assertThat(card.resolvedIpa()).isEqualTo("əˈtʃiːv");
    }
}
