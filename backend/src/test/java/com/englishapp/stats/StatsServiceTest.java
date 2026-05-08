package com.englishapp.stats;

import com.englishapp.flashcard.CardRepository;
import com.englishapp.retell.RetellAttemptRepository;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.session.SessionStatus;
import com.englishapp.shadow.UserPhonemeStatsRepository;
import com.englishapp.stats.dto.DayActivityResponse;
import com.englishapp.stats.dto.StatsOverviewResponse;
import com.englishapp.user.User;
import com.englishapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock UserRepository userRepository;
    @Mock SessionRepository sessionRepository;
    @Mock CardRepository cardRepository;
    @Mock RetellAttemptRepository retellAttemptRepository;
    @Mock UserPhonemeStatsRepository phonemeStatsRepository;
    @Mock UserEventRepository userEventRepository;

    @InjectMocks StatsService statsService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .currentStreakDays(5)
                .totalXp(200)
                .build();
    }

    @Test
    void shouldCalculateStreakCorrectly() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(sessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED)).willReturn(3L);
        given(cardRepository.countByUserIdAndStabilityGreaterThan(eq(userId), anyDouble())).willReturn(10L);
        given(retellAttemptRepository.avgScoreByUserSince(eq(userId), any())).willReturn(75.0);

        StatsOverviewResponse result = statsService.getOverview(userId);

        assertThat(result.getStreakDays()).isEqualTo(5);
        assertThat(result.getTotalXp()).isEqualTo(200);
        assertThat(result.getVideosCompleted()).isEqualTo(3L);
        assertThat(result.getVocabMastered()).isEqualTo(10L);
        assertThat(result.getAvgRetellScore7d()).isEqualTo(75.0);
    }

    @Test
    void shouldAggregateWeeklyActivity() {
        LearningSession s1 = LearningSession.builder()
                .userId(userId)
                .videoId(UUID.randomUUID())
                .status(SessionStatus.COMPLETED)
                .completedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        LearningSession s2 = LearningSession.builder()
                .userId(userId)
                .videoId(UUID.randomUUID())
                .status(SessionStatus.COMPLETED)
                .completedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        given(sessionRepository.findByUserIdAndStatusAndCompletedAtAfter(
                eq(userId), eq(SessionStatus.COMPLETED), any())).willReturn(List.of(s1, s2));
        given(userEventRepository.findByUserIdAndCreatedAtAfter(eq(userId), any())).willReturn(List.of());

        List<DayActivityResponse> result = statsService.getWeeklyActivity(userId);

        assertThat(result).hasSize(7);
        // yesterday has 2 sessions × 20 min = 40 min
        DayActivityResponse yesterday = result.get(5); // index 5 = yesterday (index 6 = today)
        assertThat(yesterday.getTotalMinutes()).isEqualTo(40);
        // today has 0 sessions
        DayActivityResponse today = result.get(6);
        assertThat(today.getTotalMinutes()).isEqualTo(0);
    }
}
