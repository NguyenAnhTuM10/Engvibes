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

    /**
     * Thêm card vào deck (mặc định: deck default của user). Idempotent — nếu card đã
     * có trong deck thì trả card cũ thay vì tạo bản trùng.
     */
    public CardResponse addCard(CreateCardRequest request, UUID userId) {
        UUID deckId = request.getDeckId() != null
                ? deckService.requireOwnedDeck(request.getDeckId(), userId).getId()
                : deckService.getOrCreateDefaultDeck(userId).getId();

        UserCard card = request.getVocabId() != null
                ? addVocabCard(request, deckId, userId)
                : addCustomCard(request, deckId, userId);
        return flashcardMapper.toCardResponse(card);
    }

    private UserCard addVocabCard(CreateCardRequest request, UUID deckId, UUID userId) {
        VocabEntry vocab = vocabRepository.findById(request.getVocabId())
                .orElseThrow(() -> ApiException.notFound("Vocab not found"));
        return cardRepository.findByUserIdAndVocab_IdAndDeckId(userId, vocab.getId(), deckId)
                .orElseGet(() -> cardRepository.save(baseCard(request, deckId, userId)
                        .vocab(vocab)
                        .build()));
    }

    private UserCard addCustomCard(CreateCardRequest request, UUID deckId, UUID userId) {
        String front = request.getFront().trim();
        return cardRepository.findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(deckId, front)
                .orElseGet(() -> cardRepository.save(baseCard(request, deckId, userId)
                        .front(front)
                        .back(request.getBack().trim())
                        .ipa(blankToNull(request.getIpa()))
                        .build()));
    }

    private UserCard.UserCardBuilder baseCard(CreateCardRequest request, UUID deckId, UUID userId) {
        return UserCard.builder()
                .userId(userId)
                .deckId(deckId)
                .contextSentence(blankToNull(request.getContextSentence()))
                .sourceVideoId(request.getSourceVideoId())
                .sourceSegmentId(request.getSourceSegmentId())
                .sourceType(request.getSourceType() != null ? request.getSourceType() : CardSource.MANUAL);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsInDeck(UUID deckId, Pageable pageable, UUID userId) {
        deckService.requireOwnedDeck(deckId, userId);
        return cardRepository.findByDeckId(deckId, pageable)
                .map(flashcardMapper::toCardResponse);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getDueCards(UUID deckId, int limit, UUID userId) {
        deckService.requireOwnedDeck(deckId, userId);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return cardRepository.findDueInDeck(deckId, Instant.now(), PageRequest.of(0, safeLimit)).stream()
                .map(flashcardMapper::toCardResponse)
                .toList();
    }

    /** Chấm một lần ôn bằng FSRS. rating: 1=Again, 2=Hard, 3=Good, 4=Easy. */
    public CardResponse reviewCard(UUID cardId, int rating, UUID userId) {
        UserCard card = requireOwnedCard(cardId, userId);
        fsrsScheduler.review(card, rating);
        return flashcardMapper.toCardResponse(cardRepository.save(card));
    }

    public void deleteCard(UUID cardId, UUID userId) {
        cardRepository.delete(requireOwnedCard(cardId, userId));
    }

    public UserCard requireOwnedCard(UUID cardId, UUID userId) {
        UserCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        if (!card.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return card;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
