package com.englishapp.flashcard;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.dto.CreateDeckRequest;
import com.englishapp.flashcard.dto.DeckResponse;
import com.englishapp.flashcard.dto.UpdateDeckRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    public DeckResponse createDeck(CreateDeckRequest request, UUID userId) {
        boolean firstDeck = !deckRepository.existsByUserIdAndIsDefaultTrue(userId);
        FlashcardDeck deck = FlashcardDeck.builder()
                .userId(userId)
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#3B82F6")
                .isDefault(firstDeck)
                .build();
        return toResponse(deckRepository.save(deck));
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> getUserDecks(UUID userId) {
        return deckRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DeckResponse updateDeck(UUID deckId, UpdateDeckRequest request, UUID userId) {
        FlashcardDeck deck = findOwned(deckId, userId);
        if (request.getName() != null) deck.setName(request.getName());
        if (request.getColor() != null) deck.setColor(request.getColor());
        return toResponse(deckRepository.save(deck));
    }

    public void deleteDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = findOwned(deckId, userId);
        if (deck.isDefault()) {
            throw ApiException.badRequest("Cannot delete the default deck");
        }
        deck.setDeletedAt(Instant.now());
        deckRepository.save(deck);
    }

    public FlashcardDeck getOrCreateDefaultDeck(UUID userId) {
        return deckRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    FlashcardDeck deck = FlashcardDeck.builder()
                            .userId(userId)
                            .name("My Deck")
                            .isDefault(true)
                            .build();
                    return deckRepository.save(deck);
                });
    }

    private FlashcardDeck findOwned(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> ApiException.notFound("Deck not found"));
        if (!deck.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return deck;
    }

    private DeckResponse toResponse(FlashcardDeck deck) {
        Instant now = Instant.now();
        return DeckResponse.builder()
                .id(deck.getId())
                .name(deck.getName())
                .color(deck.getColor())
                .isDefault(deck.isDefault())
                .cardCount(cardRepository.countByDeckId(deck.getId()))
                .dueCount(cardRepository.countByDeckIdAndNextReviewBefore(deck.getId(), now))
                .build();
    }
}
