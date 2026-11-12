package com.englishapp.game;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.flashcard.CardService;
import com.englishapp.flashcard.CardState;
import com.englishapp.flashcard.DeckService;
import com.englishapp.flashcard.UserCard;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.game.dto.AnswerResult;
import com.englishapp.game.dto.MultipleChoiceQuestion;
import com.englishapp.game.dto.TypingCheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock DeckService deckService;
    @Mock CardService cardService;
    @Mock CardRepository cardRepository;

    @InjectMocks GameService gameService;

    final UUID userId = UUID.randomUUID();
    final UUID deckId = UUID.randomUUID();

    private UserCard card(String front, String back) {
        return UserCard.builder().id(UUID.randomUUID()).userId(userId).deckId(deckId)
                .front(front).back(back).build();
    }

    private void stubReview(UUID cardId) {
        given(cardService.reviewCard(eq(cardId), anyInt(), eq(userId))).willReturn(
                CardResponse.builder().id(cardId).state(CardState.REVIEW).nextReview(Instant.now()).build());
    }

    @Test
    void levenshtein_countsEdits() {
        assertThat(GameService.levenshtein("think", "think")).isZero();
        assertThat(GameService.levenshtein("think", "tink")).isEqualTo(1);
        assertThat(GameService.levenshtein("achieve", "acheive")).isEqualTo(2);
        assertThat(GameService.levenshtein("", "abc")).isEqualTo(3);
    }

    @Test
    void checkTyping_oneCharacterTypo_isCorrectAndRatedGood() {
        UserCard c = card("Perspective", "quan điểm");
        given(cardService.requireOwnedCard(c.getId(), userId)).willReturn(c);
        stubReview(c.getId());

        TypingCheckResult result = gameService.checkTyping(c.getId(), " perspectve ", userId);

        assertThat(result.correct()).isTrue();
        assertThat(result.editDistance()).isEqualTo(1);
        assertThat(result.correctAnswer()).isEqualTo("Perspective");
        verify(cardService).reviewCard(c.getId(), GameService.RATING_CORRECT, userId);
    }

    @Test
    void checkTyping_wrongWord_isRatedAgain() {
        UserCard c = card("benefit", "lợi ích");
        given(cardService.requireOwnedCard(c.getId(), userId)).willReturn(c);
        stubReview(c.getId());

        TypingCheckResult result = gameService.checkTyping(c.getId(), "profit", userId);

        assertThat(result.correct()).isFalse();
        verify(cardService).reviewCard(c.getId(), GameService.RATING_WRONG, userId);
    }

    @Test
    void checkAnswer_comparesAgainstStoredMeaning_notClientData() {
        UserCard c = card("evidence", "bằng chứng");
        given(cardService.requireOwnedCard(c.getId(), userId)).willReturn(c);
        stubReview(c.getId());

        AnswerResult result = gameService.checkAnswer(c.getId(), "BẰNG CHỨNG ", userId);

        assertThat(result.correct()).isTrue();
        assertThat(result.correctAnswer()).isEqualTo("bằng chứng");
        assertThat(result.state()).isEqualTo(CardState.REVIEW);
    }

    @Test
    void multipleChoice_eachQuestionHasFourDistinctOptionsIncludingTheAnswer() {
        List<UserCard> cards = List.of(
                card("achieve", "đạt được"), card("assume", "giả định"),
                card("benefit", "lợi ích"), card("evidence", "bằng chứng"), card("indicate", "chỉ ra"));
        given(cardRepository.findAllInDeckWithVocab(deckId)).willReturn(cards);

        List<MultipleChoiceQuestion> questions = gameService.generateMultipleChoice(deckId, 10, userId);

        assertThat(questions).hasSize(cards.size());
        for (MultipleChoiceQuestion q : questions) {
            String answer = cards.stream().filter(c -> c.getId().equals(q.cardId()))
                    .findFirst().orElseThrow().getBack();
            assertThat(q.options()).hasSize(4).doesNotHaveDuplicates().contains(answer);
        }
        verify(deckService).requireOwnedDeck(deckId, userId);
    }

    @Test
    void multipleChoice_needsAtLeastFourCards() {
        given(cardRepository.findAllInDeckWithVocab(deckId))
                .willReturn(List.of(card("a", "1"), card("b", "2"), card("c", "3")));

        assertThatThrownBy(() -> gameService.generateMultipleChoice(deckId, 10, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least 4");
    }
}
