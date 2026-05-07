package com.englishapp.video.subtitle;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subtitle_segments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID videoId;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private int startMs;

    @Column(nullable = false)
    private int endMs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    // JSON array: [{"word":"hello","startMs":0,"endMs":500},...]
    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String wordTimings = "[]";

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
