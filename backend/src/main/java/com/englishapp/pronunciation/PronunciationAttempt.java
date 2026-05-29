package com.englishapp.pronunciation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pronunciation_attempts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PronunciationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    @Builder.Default
    private int attemptNumber = 1;

    @Column(columnDefinition = "TEXT")
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcript;          // Whisper output

    @Column(columnDefinition = "TEXT")
    private String actualIpa;

    private Integer overallScore;
    private Integer accuracyScore;
    private Integer fluencyScore;

    // JSON array: [{position, expected, actual, matched, tip}]
    @Column(columnDefinition = "JSONB")
    private String phonemeDetail;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
