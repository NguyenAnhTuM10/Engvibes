package com.englishapp.sm2;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Sm2CardRepository extends JpaRepository<Sm2Card, UUID> {
    List<Sm2Card> findByDeckId(UUID deckId);
    Optional<Sm2Card> findFirstByDeckIdAndFrontIgnoreCase(UUID deckId, String front);
}
