package com.englishapp.sm2;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Sm2DeckRepository extends JpaRepository<Sm2Deck, UUID> {
    Optional<Sm2Deck> findFirstByName(String name);
}
