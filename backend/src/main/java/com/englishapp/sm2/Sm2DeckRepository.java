package com.englishapp.sm2;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface Sm2DeckRepository extends JpaRepository<Sm2Deck, UUID> {}
