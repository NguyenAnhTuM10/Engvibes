package com.englishapp.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, UUID> {
}
