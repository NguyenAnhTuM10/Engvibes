package com.englishapp.recommend;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_video_interaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVideoInteraction {

    @EmbeddedId
    private UserVideoInteractionId id;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "completion_score", nullable = false)
    @Builder.Default
    private double completionScore = 0.0;

    @Column(name = "last_viewed")
    private Instant lastViewed;
}
