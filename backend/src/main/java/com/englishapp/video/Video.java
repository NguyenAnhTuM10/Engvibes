package com.englishapp.video;

import com.englishapp.user.CEFRLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "videos")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String storageUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private CEFRLevel cefrLevel;

    @Column(length = 50)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VideoStatus status = VideoStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private double vocabDifficulty = 0;

    @Column(nullable = false)
    @Builder.Default
    private double popularityScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String warmupWordsJson = "[]";

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String collocationsJson = "{}";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String keyPointsJson = "[]";

    @Column(columnDefinition = "TEXT")
    private String speakingQuestion;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant deletedAt;
}
