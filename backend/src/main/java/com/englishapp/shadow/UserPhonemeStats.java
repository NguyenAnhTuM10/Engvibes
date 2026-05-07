package com.englishapp.shadow;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_phoneme_stats")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPhonemeStats {

    @EmbeddedId
    private UserPhonemeStatsId id;

    @Column(nullable = false)
    @Builder.Default
    private int totalAttempts = 0;

    @Column(nullable = false)
    @Builder.Default
    private int errors = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant lastUpdated = Instant.now();
}
