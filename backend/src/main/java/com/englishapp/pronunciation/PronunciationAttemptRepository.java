package com.englishapp.pronunciation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PronunciationAttemptRepository extends JpaRepository<PronunciationAttempt, UUID> {

    int countBySessionId(UUID sessionId);

    List<PronunciationAttempt> findBySessionIdOrderByCreatedAt(UUID sessionId);
}
