package com.englishapp.stats;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.retell.RetellAttemptRepository;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.session.SessionStatus;
import com.englishapp.shadow.UserPhonemeStats;
import com.englishapp.shadow.UserPhonemeStatsRepository;
import com.englishapp.stats.dto.*;
import com.englishapp.user.CEFRLevel;
import com.englishapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final int AVG_SESSION_MINUTES = 20;
    private static final double MASTERED_STABILITY_THRESHOLD = 30.0;

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final CardRepository cardRepository;
    private final RetellAttemptRepository retellAttemptRepository;
    private final UserPhonemeStatsRepository phonemeStatsRepository;
    private final UserEventRepository userEventRepository;

    @Transactional(readOnly = true)
    public StatsOverviewResponse getOverview(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        long videosCompleted = sessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED);
        long vocabMastered = cardRepository.countByUserIdAndStabilityGreaterThan(userId, MASTERED_STABILITY_THRESHOLD);

        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        Double avgRetell = retellAttemptRepository.avgScoreByUserSince(userId, sevenDaysAgo);

        return StatsOverviewResponse.builder()
                .streakDays(user.getCurrentStreakDays())
                .totalXp(user.getTotalXp())
                .videosCompleted(videosCompleted)
                .vocabMastered(vocabMastered)
                .avgRetellScore7d(avgRetell != null ? avgRetell : 0.0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DayActivityResponse> getWeeklyActivity(UUID userId) {
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));

        // Sessions completed in last 7 days — estimate 20 min each
        List<LearningSession> sessions = sessionRepository
                .findByUserIdAndStatusAndCompletedAtAfter(userId, SessionStatus.COMPLETED, sevenDaysAgo);
        Map<LocalDate, Integer> sessionsByDay = new TreeMap<>();
        for (LearningSession s : sessions) {
            if (s.getCompletedAt() != null) {
                LocalDate date = s.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate();
                sessionsByDay.merge(date, 1, Integer::sum);
            }
        }

        // Events in last 7 days — aggregate by day and type
        List<UserEvent> events = userEventRepository.findByUserIdAndCreatedAtAfter(userId, sevenDaysAgo);
        Map<LocalDate, Map<String, Integer>> eventsByDay = new TreeMap<>();
        for (UserEvent e : events) {
            LocalDate date = e.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            eventsByDay.computeIfAbsent(date, k -> new HashMap<>())
                       .merge(e.getEventType(), 1, Integer::sum);
        }

        // Build last 7 days (include days with 0 activity)
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<DayActivityResponse> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int sessCount = sessionsByDay.getOrDefault(date, 0);
            Map<String, Integer> dayEvents = eventsByDay.getOrDefault(date, Map.of());

            Map<String, Integer> byActivity = new LinkedHashMap<>();
            byActivity.put("listen",  dayEvents.getOrDefault("listen",  0));
            byActivity.put("shadow",  dayEvents.getOrDefault("shadow",  0));
            byActivity.put("retell",  dayEvents.getOrDefault("retell",  0));
            byActivity.put("speak",   dayEvents.getOrDefault("speak",   0));

            result.add(DayActivityResponse.builder()
                    .date(date)
                    .totalMinutes(sessCount * AVG_SESSION_MINUTES)
                    .byActivity(byActivity)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PhonemeStatsResponse> getPhonemeStats(UUID userId) {
        return phonemeStatsRepository.findByIdUserId(userId).stream()
                .filter(s -> s.getTotalAttempts() >= 5)
                .map(s -> {
                    double errorRate = s.getTotalAttempts() > 0
                            ? (double) s.getErrors() / s.getTotalAttempts()
                            : 0.0;
                    return PhonemeStatsResponse.builder()
                            .phoneme(s.getId().getPhoneme())
                            .totalAttempts(s.getTotalAttempts())
                            .errors(s.getErrors())
                            .errorRate(errorRate)
                            .build();
                })
                .sorted(Comparator.comparingDouble(PhonemeStatsResponse::getErrorRate).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, Map<CEFRLevel, Integer>> getVocabGrowth(UUID userId) {
        List<Object[]> rows = cardRepository.findCreatedAtAndCefrByUserId(userId);

        // daily counts
        Map<LocalDate, Map<CEFRLevel, Integer>> dailyCounts = new TreeMap<>();
        for (Object[] row : rows) {
            Instant createdAt = (Instant) row[0];
            CEFRLevel cefr = (CEFRLevel) row[1];
            LocalDate date = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            dailyCounts.computeIfAbsent(date, k -> new EnumMap<>(CEFRLevel.class))
                       .merge(cefr, 1, Integer::sum);
        }

        // cumulative
        Map<CEFRLevel, Integer> running = new EnumMap<>(CEFRLevel.class);
        Map<LocalDate, Map<CEFRLevel, Integer>> cumulative = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Map<CEFRLevel, Integer>> entry : dailyCounts.entrySet()) {
            entry.getValue().forEach((cefr, count) -> running.merge(cefr, count, Integer::sum));
            cumulative.put(entry.getKey(), new EnumMap<>(running));
        }

        return cumulative;
    }
}
