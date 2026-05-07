package com.englishapp.session;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID videoId;

    @Column(nullable = false)
    @Builder.Default
    private int currentStep = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String completedSteps = "[]";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    private Integer scaffoldLevel;

    @Column(nullable = false)
    @Builder.Default
    private int totalXpEarned = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    private Instant completedAt;
}
