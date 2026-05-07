package com.englishapp.flashcard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<FlashcardDeck, UUID> {
    List<FlashcardDeck> findByUserId(UUID userId);
    Optional<FlashcardDeck> findByUserIdAndIsDefaultTrue(UUID userId);
    boolean existsByUserIdAndIsDefaultTrue(UUID userId);
}
