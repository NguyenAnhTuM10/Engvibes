package com.englishapp.vocab;

import com.englishapp.user.CEFRLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vocab_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private CEFRLevel cefrLevel;

    @Column(length = 20)
    private String partOfSpeech;

    @Column(length = 200)
    private String ipa;

    @Column(columnDefinition = "TEXT")
    private String phonemes;

    @Column(columnDefinition = "TEXT")
    private String definition;

    private Integer frequencyRank;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
