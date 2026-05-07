package com.englishapp.session.phrase;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phrase_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhraseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int phraseIdx;

    @Column(length = 500)
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(nullable = false)
    @Builder.Default
    private double accuracyScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
