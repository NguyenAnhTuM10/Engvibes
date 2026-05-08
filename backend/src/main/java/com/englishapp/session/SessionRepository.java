package com.englishapp.session;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<LearningSession, UUID> {
    Optional<LearningSession> findByUserIdAndVideoId(UUID userId, UUID videoId);
    Page<LearningSession> findByUserId(UUID userId, Pageable pageable);
    List<LearningSession> findTop10ByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, SessionStatus status);
    long countByUserIdAndStatus(UUID userId, SessionStatus status);
}
