package com.englishapp.retell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RetellAttemptRepository extends JpaRepository<RetellAttempt, UUID> {
    List<RetellAttempt> findBySessionIdOrderByCreatedAt(UUID sessionId);
    int countBySessionId(UUID sessionId);

    @Query(value = """
            SELECT AVG(r.overall_score) FROM retell_attempts r
            JOIN learning_sessions s ON r.session_id = s.id
            WHERE s.user_id = :userId AND r.created_at >= :since
            """, nativeQuery = true)
    Double avgScoreByUserSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
