package com.englishapp.flashcard;

import com.englishapp.vocab.VocabEntry;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    private VocabEntry vocab;

    @Column(nullable = false)
    private UUID deckId;

    @Column(nullable = false)
    @Builder.Default
    private double stability = 0;

    @Column(nullable = false)
    @Builder.Default
    private double difficulty = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CardState state = CardState.NEW;

    private Instant lastReview;
    private Instant nextReview;

    @Column(nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int lapseCount = 0;

    @Column(columnDefinition = "TEXT")
    private String contextSentence;

    private UUID sourceVideoId;
    private UUID sourceSegmentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CardSource sourceType;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
