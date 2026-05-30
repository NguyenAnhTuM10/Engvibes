package com.englishapp.sm2;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface Sm2CardRepository extends JpaRepository<Sm2Card, UUID> {
    List<Sm2Card> findByDeckId(UUID deckId);
}
