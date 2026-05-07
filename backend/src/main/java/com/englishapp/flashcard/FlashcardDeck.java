package com.englishapp.flashcard;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_decks")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 7)
    @Builder.Default
    private String color = "#3B82F6";

    @Column(nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant deletedAt;
}
