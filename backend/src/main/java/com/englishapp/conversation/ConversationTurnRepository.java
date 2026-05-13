package com.englishapp.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationTurnRepository extends JpaRepository<ConversationTurn, UUID> {
    List<ConversationTurn> findBySessionIdOrderByTurnNumber(UUID sessionId);
}
