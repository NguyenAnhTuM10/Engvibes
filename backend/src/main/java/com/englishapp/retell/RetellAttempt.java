package com.englishapp.retell;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retell_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetellAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(length = 500)
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(nullable = false)
    @Builder.Default
    private int scaffoldLevel = 1;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(nullable = false)
    @Builder.Default
    private int overallScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private int durationSec = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
