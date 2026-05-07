package com.englishapp.retell;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetellAttemptRepository extends JpaRepository<RetellAttempt, UUID> {
    List<RetellAttempt> findBySessionIdOrderByCreatedAt(UUID sessionId);
    int countBySessionId(UUID sessionId);
}
