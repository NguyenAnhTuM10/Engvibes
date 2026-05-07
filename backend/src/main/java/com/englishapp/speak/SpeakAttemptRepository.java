package com.englishapp.speak;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpeakAttemptRepository extends JpaRepository<SpeakAttempt, UUID> {
    List<SpeakAttempt> findBySessionIdOrderByCreatedAt(UUID sessionId);
}
