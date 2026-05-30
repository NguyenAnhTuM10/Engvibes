package com.englishapp.conversation;

import com.englishapp.user.User;
import com.englishapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * T2: @Transactional bridge между ConversationProxyWebSocketHandler (WS thread)
 *     và JPA repositories. Proxy gọi các method này để persist session/turns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationRealtimeService {

    private static final int XP_PER_TURN = 5;

    private final ConversationSessionRepository sessionRepo;
    private final ConversationTurnRepository    turnRepo;
    private final UserRepository                userRepo;

    /** T2.1 — Tạo session khi WS mở. */
    @Transactional
    public ConversationSession createSession(UUID userId, String scenarioId) {
        ConversationSession session = ConversationSession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .status("ACTIVE")
                .build();
        return sessionRepo.save(session);
    }

    /**
     * T2.1 + T2.2 — Hoàn thành session khi WS đóng:
     *   1. Persist ConversationTurn cho mỗi transcript entry (user + AI).
     *   2. Update ConversationSession: COMPLETED, turns, XP, endedAt.
     *   3. Update User: totalXp, streak.
     *
     * @param entries list (role→"user"|"assistant", text) thu thập từ proxy
     */
    @Transactional
    public void completeSession(UUID sessionId, UUID userId, List<TranscriptEntry> entries) {
        ConversationSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[realtime-svc] Session not found on complete: {}", sessionId);
            return;
        }

        // Persist turns
        int turnNum = 0;
        for (TranscriptEntry e : entries) {
            turnNum++;
            ConversationTurn turn = ConversationTurn.builder()
                    .sessionId(sessionId)
                    .turnNumber(turnNum)
                    .userTranscript("user".equals(e.role()) ? e.text() : "")
                    .aiText("assistant".equals(e.role()) ? e.text() : "")
                    .build();
            turnRepo.save(turn);
        }

        // Count actual user turns for XP
        long userTurns = entries.stream().filter(e -> "user".equals(e.role())).count();
        int xp = (int) (userTurns * XP_PER_TURN);

        session.setStatus("COMPLETED");
        session.setTotalTurns((int) userTurns);
        session.setXpEarned(xp);
        session.setEndedAt(Instant.now());
        sessionRepo.save(session);

        // Update User XP + streak
        userRepo.findById(userId).ifPresent(user -> {
            user.setTotalXp(user.getTotalXp() + xp);
            updateStreak(user);
            userRepo.save(user);
            log.info("[realtime-svc] session={} user={} userTurns={} xp={} streak={}",
                    sessionId, userId, userTurns, xp, user.getCurrentStreakDays());
        });
    }

    /**
     * T2.2 — Lưu review JSON vào session (gọi sau /realtime-review).
     */
    @Transactional
    public void saveReview(UUID sessionId, String reviewJson) {
        sessionRepo.findById(sessionId).ifPresent(s -> {
            s.setSummary(reviewJson);
            sessionRepo.save(s);
        });
    }

    /**
     * T2.2 — Load user transcript từ server-observed turns (authoritative).
     * Chỉ lấy turns có userTranscript không rỗng.
     */
    @Transactional(readOnly = true)
    public List<String> loadUserTranscripts(UUID sessionId) {
        return turnRepo.findBySessionIdOrderByTurnNumber(sessionId).stream()
                .map(ConversationTurn::getUserTranscript)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSession> getSessionHistory(UUID userId) {
        return sessionRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** T2.2 — Lấy session có kiểm tra ownership. Dùng khi chấm review. */
    @Transactional(readOnly = true)
    public ConversationSession getOwnedSession(UUID sessionId, UUID userId) {
        ConversationSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> com.englishapp.common.ApiException.notFound("Session not found"));
        if (!s.getUserId().equals(userId)) {
            throw com.englishapp.common.ApiException.forbidden("Access denied to session " + sessionId);
        }
        return s;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();
        if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
            user.setCurrentStreakDays(1);
        } else if (lastActive.equals(today.minusDays(1))) {
            user.setCurrentStreakDays(user.getCurrentStreakDays() + 1);
        }
        // lastActive == today: already counted, no change
        user.setLastActiveDate(today);
    }

    /** Immutable entry: transcript của 1 lượt nói (user hoặc AI). */
    public record TranscriptEntry(String role, String text) {}
}
