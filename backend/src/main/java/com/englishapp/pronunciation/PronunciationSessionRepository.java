package com.englishapp.pronunciation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PronunciationSessionRepository extends JpaRepository<PronunciationSession, UUID> {
}
