package com.englishapp.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    @Builder.Default
    private CEFRLevel cefrLevel = CEFRLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    @Builder.Default
    private CEFRLevel workingCefrLevel = CEFRLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private int totalXp = 0;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreakDays = 0;

    private LocalDate lastActiveDate;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant deletedAt;
}
