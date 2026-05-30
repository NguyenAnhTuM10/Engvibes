package com.englishapp.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, UUID> {
    List<ConversationSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
