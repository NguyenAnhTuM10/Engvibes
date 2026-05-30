package com.englishapp.sm2;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sm2_cards")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Sm2Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID deckId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String front;           // từ / English

    @Column(nullable = false, columnDefinition = "TEXT")
    private String back;            // nghĩa / Vietnamese

    @Column(length = 255)
    private String ipa;

    @Column(columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
