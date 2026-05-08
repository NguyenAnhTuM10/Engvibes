package com.englishapp.recommend;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.session.SessionStatus;
import com.englishapp.shadow.UserPhonemeStats;
import com.englishapp.shadow.UserPhonemeStatsRepository;
import com.englishapp.user.CEFRLevel;
import com.englishapp.user.UserRepository;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFeatureService {

    private static final int MIN_PHONEME_ATTEMPTS = 5;
    private static final int RECENT_SESSION_LIMIT = 10;

    private final UserRepository userRepository;
    private final UserPhonemeStatsRepository phonemeStatsRepository;
    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public UserFeatureVector buildUserFeatureVector(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        CEFRLevel cefrLevel = user.getWorkingCefrLevel() != null
                ? user.getWorkingCefrLevel()
                : user.getCefrLevel();

        List<String> weakPhonemes = buildWeakPhonemes(userId);
        List<String> recentTopics = buildRecentTopics(userId);
        Map<CEFRLevel, Integer> vocabKnownByCefr = buildVocabKnownByCefr(userId);
        long activeSessionCount = sessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED);

        return new UserFeatureVector(
                cefrLevel,
                weakPhonemes,
                recentTopics,
                vocabKnownByCefr,
                (int) activeSessionCount
        );
    }

    private List<String> buildWeakPhonemes(UUID userId) {
        return phonemeStatsRepository.findByIdUserId(userId).stream()
                .filter(s -> s.getTotalAttempts() >= MIN_PHONEME_ATTEMPTS)
                .filter(s -> s.getTotalAttempts() > 0)
                .sorted(Comparator.comparingDouble(s -> -((double) s.getErrors() / s.getTotalAttempts())))
                .limit(5)
                .map(s -> s.getId().getPhoneme())
                .collect(Collectors.toList());
    }

    private List<String> buildRecentTopics(UUID userId) {
        List<LearningSession> recentSessions = sessionRepository
                .findTop10ByUserIdAndStatusOrderByCompletedAtDesc(userId, SessionStatus.COMPLETED);

        if (recentSessions.isEmpty()) return List.of();

        Set<UUID> videoIds = recentSessions.stream()
                .map(LearningSession::getVideoId)
                .collect(Collectors.toSet());

        Map<UUID, String> topicByVideo = videoRepository.findAllById(videoIds).stream()
                .filter(v -> v.getTopic() != null)
                .collect(Collectors.toMap(Video::getId, Video::getTopic));

        // preserve order, deduplicate
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        recentSessions.stream()
                .map(s -> topicByVideo.get(s.getVideoId()))
                .filter(Objects::nonNull)
                .forEach(topics::add);

        return new ArrayList<>(topics);
    }

    private Map<CEFRLevel, Integer> buildVocabKnownByCefr(UUID userId) {
        Map<CEFRLevel, Integer> result = new EnumMap<>(CEFRLevel.class);
        for (Object[] row : cardRepository.countByUserIdGroupByCefrLevel(userId)) {
            result.put((CEFRLevel) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }
}
