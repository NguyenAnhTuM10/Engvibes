package com.englishapp.shadow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShadowAttemptRepository extends JpaRepository<ShadowAttempt, UUID> {
    List<ShadowAttempt> findBySessionIdOrderByCreatedAt(UUID sessionId);
    List<ShadowAttempt> findBySessionIdAndSegmentId(UUID sessionId, UUID segmentId);
    int countBySessionIdAndSegmentId(UUID sessionId, UUID segmentId);
}
