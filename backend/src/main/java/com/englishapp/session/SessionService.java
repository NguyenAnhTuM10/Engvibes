package com.englishapp.session;

import com.englishapp.common.ApiException;
import com.englishapp.session.dto.*;
import com.englishapp.user.User;
import com.englishapp.user.UserRepository;
import com.englishapp.video.VideoRepository;
import com.englishapp.video.VideoStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int XP_PER_STEP = 5;

    public SessionResponse getOrCreateSession(UUID userId, UUID videoId) {
        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw ApiException.badRequest("Video is not available for learning");
        }
        return sessionRepository.findByUserIdAndVideoId(userId, videoId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    LearningSession session = LearningSession.builder()
                            .userId(userId)
                            .videoId(videoId)
                            .build();
                    return toResponse(sessionRepository.save(session));
                });
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID userId) {
        return toResponse(requireOwned(sessionId, userId));
    }

    public SessionResponse advanceStep(UUID sessionId, UUID userId, AdvanceStepRequest request) {
        LearningSession session = requireOwned(sessionId, userId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw ApiException.badRequest("Session is already completed");
        }
        int step = request.getStep();
        List<Integer> steps = parseSteps(session.getCompletedSteps());
        if (!steps.contains(step)) {
            steps.add(step);
            session.setCompletedSteps(serializeSteps(steps));
        }
        if ("complete".equals(request.getAction())) {
            session.setCurrentStep(Math.min(step + 1, 6));
            session.setTotalXpEarned(session.getTotalXpEarned() + XP_PER_STEP);
        }
        return toResponse(sessionRepository.save(session));
    }

    public SessionResponse setScaffoldLevel(UUID sessionId, UUID userId, SetScaffoldRequest request) {
        LearningSession session = requireOwned(sessionId, userId);
        session.setScaffoldLevel(request.getLevel());
        return toResponse(sessionRepository.save(session));
    }

    public SessionResponse finishSession(UUID sessionId, UUID userId) {
        LearningSession session = requireOwned(sessionId, userId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return toResponse(session);
        }
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        LearningSession saved = sessionRepository.save(session);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setTotalXp(user.getTotalXp() + saved.getTotalXpEarned());
        updateStreak(user);
        userRepository.save(user);

        log.info("Session {} finished — xp={}, streak={}", sessionId, saved.getTotalXpEarned(), user.getCurrentStreakDays());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> getHistory(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();
        if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
            user.setCurrentStreakDays(1);
        } else if (lastActive.equals(today.minusDays(1))) {
            user.setCurrentStreakDays(user.getCurrentStreakDays() + 1);
        }
        // lastActive == today: already counted today, no change
        user.setLastActiveDate(today);
    }

    private LearningSession requireOwned(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return session;
    }

    private SessionResponse toResponse(LearningSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .videoId(s.getVideoId())
                .currentStep(s.getCurrentStep())
                .completedSteps(parseSteps(s.getCompletedSteps()))
                .status(s.getStatus())
                .scaffoldLevel(s.getScaffoldLevel())
                .totalXpEarned(s.getTotalXpEarned())
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }

    private List<Integer> parseSteps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeSteps(List<Integer> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "[]";
        }
    }
}
