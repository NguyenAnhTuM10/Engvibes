package com.englishapp.flashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<UserCard, UUID> {
    Page<UserCard> findByDeckId(UUID deckId, Pageable pageable);
    Page<UserCard> findByDeckIdAndNextReviewBefore(UUID deckId, Instant now, Pageable pageable);
    Optional<UserCard> findByUserIdAndVocab_IdAndDeckId(UUID userId, UUID vocabId, UUID deckId);
    long countByDeckId(UUID deckId);
    long countByDeckIdAndNextReviewBefore(UUID deckId, Instant now);
    long countByUserIdAndStateNot(UUID userId, CardState state);
}
