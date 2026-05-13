package com.englishapp.conversation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_turns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int turnNumber;

    @Column(length = 500)
    private String userAudioKey;

    @Column(columnDefinition = "TEXT")
    private String userTranscript;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiText;

    @Column(length = 500)
    private String aiAudioKey;

    @Column(columnDefinition = "TEXT")
    private String hintsJson;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
