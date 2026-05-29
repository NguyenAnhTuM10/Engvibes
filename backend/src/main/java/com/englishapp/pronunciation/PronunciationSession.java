package com.englishapp.pronunciation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pronunciation_sessions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PronunciationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetText;      // từ cần phát âm, vd: "think"

    @Column(columnDefinition = "TEXT")
    private String targetIpa;       // được back-fill sau lần attempt đầu tiên

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String sessionType = "WORD";

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
