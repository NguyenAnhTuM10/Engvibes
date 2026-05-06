package com.englishapp.demo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demo_pings")
@Data
@NoArgsConstructor
public class DemoPing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}
