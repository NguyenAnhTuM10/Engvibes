package com.englishapp.flashcard;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.flashcard.dto.CreateCardRequest;
import com.englishapp.vocab.VocabEntry;
import com.englishapp.vocab.VocabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final DeckService deckService;
    private final VocabRepository vocabRepository;
    private final FsrsScheduler fsrsScheduler;
    private final FlashcardMapper flashcardMapper;

    public CardResponse addCard(CreateCardRequest request, UUID userId) {
        VocabEntry vocab = vocabRepository.findById(request.getVocabId())
                .orElseThrow(() -> ApiException.notFound("Vocab not found"));

        UUID deckId = request.getDeckId() != null
                ? request.getDeckId()
                : deckService.getOrCreateDefaultDeck(userId).getId();

        // Idempotent — return existing if already added
        return cardRepository.findByUserIdAndVocab_IdAndDeckId(userId, vocab.getId(), deckId)
                .map(flashcardMapper::toCardResponse)
                .orElseGet(() -> {
                    UserCard card = UserCard.builder()
                            .userId(userId)
                            .vocab(vocab)
                            .deckId(deckId)
                            .contextSentence(request.getContextSentence())
                            .sourceVideoId(request.getSourceVideoId())
                            .sourceSegmentId(request.getSourceSegmentId())
                            .sourceType(request.getSourceType())
                            .build();
                    return flashcardMapper.toCardResponse(cardRepository.save(card));
                });
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsInDeck(UUID deckId, Pageable pageable, UUID userId) {
        return cardRepository.findByDeckId(deckId, pageable)
                .map(flashcardMapper::toCardResponse);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getDueCards(UUID deckId, int limit) {
        return cardRepository.findByDeckIdAndNextReviewBefore(deckId, Instant.now(), PageRequest.of(0, limit))
                .map(flashcardMapper::toCardResponse)
                .toList();
    }

    public CardResponse reviewCard(UUID cardId, int rating, UUID userId) {
        UserCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        if (!card.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        fsrsScheduler.review(card, rating);
        return flashcardMapper.toCardResponse(cardRepository.save(card));
    }

    public void deleteCard(UUID cardId, UUID userId) {
        UserCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        if (!card.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        cardRepository.delete(card);
    }
}
