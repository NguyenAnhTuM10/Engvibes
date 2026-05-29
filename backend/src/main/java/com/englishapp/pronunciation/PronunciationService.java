package com.englishapp.pronunciation;

import com.englishapp.common.ApiException;
import com.englishapp.pronunciation.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationService {

    private final PronunciationSessionRepository sessionRepo;
    private final PronunciationAttemptRepository attemptRepo;
    private final ObjectMapper objectMapper;

    // ── Sessions ─────────────────────────────────────────────────────────

    @Transactional
    public SessionResponse createSession(UUID userId, CreateSessionRequest req) {
        PronunciationSession session = PronunciationSession.builder()
                .userId(userId)
                .targetText(req.targetText().trim())
                .targetIpa(req.targetIpa())
                .sessionType(req.sessionType() != null ? req.sessionType() : "WORD")
                .build();
        PronunciationSession saved = sessionRepo.save(session);
        return toSessionResponse(saved, 0, null);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID userId) {
        PronunciationSession session = requireOwned(sessionId, userId);
        List<PronunciationAttempt> attempts = attemptRepo.findBySessionIdOrderByCreatedAt(sessionId);
        int bestScore = attempts.stream()
                .filter(a -> a.getOverallScore() != null)
                .mapToInt(PronunciationAttempt::getOverallScore)
                .max().orElse(0);
        return toSessionResponse(session, attempts.size(), attempts.isEmpty() ? null : bestScore);
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAttempts(UUID sessionId, UUID userId) {
        requireOwned(sessionId, userId);
        return attemptRepo.findBySessionIdOrderByCreatedAt(sessionId).stream()
                .filter(a -> a.getOverallScore() != null)  // bỏ placeholder chưa có kết quả
                .map(this::toAttemptResponse)
                .toList();
    }

    // ── Attempt lifecycle ─────────────────────────────────────────────────

    /** Tạo placeholder row để lấy attemptId trả về ngay cho client. */
    @Transactional
    public UUID createAttemptPlaceholder(UUID sessionId, UUID userId) {
        requireOwned(sessionId, userId);
        int nextNum = attemptRepo.countBySessionId(sessionId) + 1;
        PronunciationAttempt placeholder = PronunciationAttempt.builder()
                .sessionId(sessionId)
                .attemptNumber(nextNum)
                .build();
        return attemptRepo.save(placeholder).getId();
    }

    /** Điền kết quả vào placeholder sau khi pipeline xử lý xong. */
    @Transactional
    public AttemptResponse saveAttemptResult(UUID attemptId, String audioUrl,
                                             String transcript, AnalyzeResult result) {
        PronunciationAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> ApiException.notFound("Attempt not found"));

        attempt.setAudioUrl(audioUrl);
        attempt.setTranscript(transcript);
        attempt.setActualIpa(result.actualIpa());
        attempt.setOverallScore(result.overallScore());
        attempt.setAccuracyScore(result.accuracyScore());
        attempt.setFluencyScore(result.fluencyScore());
        attempt.setPhonemeDetail(serializePhonemeMatches(result));
        attemptRepo.save(attempt);

        // Back-fill targetIpa trên session để các attempt sau không cần tính lại
        if (result.targetIpa() != null) {
            PronunciationSession session = sessionRepo.findById(attempt.getSessionId()).orElseThrow();
            if (session.getTargetIpa() == null) {
                session.setTargetIpa(result.targetIpa());
                sessionRepo.save(session);
            }
        }

        return toAttemptResponse(attempt, result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public PronunciationSession requireOwned(UUID sessionId, UUID userId) {
        PronunciationSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Pronunciation session not found"));
        if (!s.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return s;
    }

    private SessionResponse toSessionResponse(PronunciationSession s, int count, Integer bestScore) {
        return new SessionResponse(s.getId(), s.getTargetText(), s.getTargetIpa(),
                s.getSessionType(), count, bestScore, s.getCreatedAt());
    }

    private AttemptResponse toAttemptResponse(PronunciationAttempt a) {
        List<PhonemeMatch> phonemes = parsePhonemeDetail(a.getPhonemeDetail());
        return new AttemptResponse(a.getId(), a.getAttemptNumber(), a.getTranscript(),
                null, a.getActualIpa(),
                a.getOverallScore() != null ? a.getOverallScore() : 0,
                a.getAccuracyScore() != null ? a.getAccuracyScore() : 0,
                a.getFluencyScore() != null ? a.getFluencyScore() : 0,
                phonemes, a.getCreatedAt());
    }

    private AttemptResponse toAttemptResponse(PronunciationAttempt a, AnalyzeResult result) {
        List<PhonemeMatch> phonemes = result.phonemeMatches() != null
                ? result.phonemeMatches().stream()
                    .map(m -> new PhonemeMatch(m.position(), m.expected(), m.actual(), m.matched(), m.tip()))
                    .toList()
                : List.of();
        return new AttemptResponse(a.getId(), a.getAttemptNumber(), a.getTranscript(),
                result.targetIpa(), result.actualIpa(),
                result.overallScore(), result.accuracyScore(), result.fluencyScore(),
                phonemes, a.getCreatedAt());
    }

    private String serializePhonemeMatches(AnalyzeResult result) {
        try {
            return objectMapper.writeValueAsString(
                    result.phonemeMatches() != null
                            ? result.phonemeMatches().stream()
                                .map(m -> new PhonemeMatch(m.position(), m.expected(), m.actual(), m.matched(), m.tip()))
                                .toList()
                            : List.of()
            );
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<PhonemeMatch> parsePhonemeDetail(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
