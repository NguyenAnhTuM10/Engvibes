package com.englishapp.shadow;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shadow_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID segmentId;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(length = 500)
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String wordDiff = "[]";

    @Column(nullable = false)
    @Builder.Default
    private double accuracyScore = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String weakPhonemesDetected = "[]";

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
