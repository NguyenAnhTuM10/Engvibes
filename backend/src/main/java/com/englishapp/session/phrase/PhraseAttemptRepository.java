package com.englishapp.session.phrase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhraseAttemptRepository extends JpaRepository<PhraseAttempt, UUID> {
    List<PhraseAttempt> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
