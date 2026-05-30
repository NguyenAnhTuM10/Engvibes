package com.englishapp.sm2;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sm2_decks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Sm2Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private UUID ownerId;   // null = demo mode

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
