package com.englishapp.speak;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "speak_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(length = 500)
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(nullable = false)
    @Builder.Default
    private int overallScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
