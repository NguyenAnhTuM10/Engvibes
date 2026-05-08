package com.englishapp.stats;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findByUserIdAndCreatedAtAfter(UUID userId, Instant after);

    long countByUserIdAndEventType(UUID userId, String eventType);
}
