package com.englishapp.recommend;

import com.englishapp.user.CEFRLevel;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import com.englishapp.video.VideoService;
import com.englishapp.video.VideoStatus;
import com.englishapp.video.dto.VideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentBasedRecommender {

    private final UserFeatureService userFeatureService;
    private final VideoRepository videoRepository;
    private final UserVideoInteractionRepository interactionRepository;
    private final VideoService videoService;

    @Transactional(readOnly = true)
    public List<VideoResponse> recommend(UUID userId, int limit) {
        UserFeatureVector user = userFeatureService.buildUserFeatureVector(userId);

        List<Video> allPublished = videoRepository.findAllByStatus(VideoStatus.PUBLISHED);
        Set<UUID> completedIds = interactionRepository.findCompletedVideoIdsByUserId(userId);
        Set<UUID> seenIds = interactionRepository.findAllVideoIdsByUserId(userId);

        List<Video> candidates = allPublished.stream()
                .filter(v -> !completedIds.contains(v.getId()))
                .toList();

        int maxViewCount = candidates.stream().mapToInt(Video::getViewCount).max().orElse(1);

        return candidates.stream()
                .map(v -> {
                    VideoFeatureVector vfv = buildVideoFeatureVector(v, maxViewCount);
                    double score = computeScore(user, vfv, seenIds);
                    log.debug("Video {} topic={} cefr={} score={}", v.getId(), v.getTopic(), v.getCefrLevel(), score);
                    return Map.entry(v, score);
                })
                .sorted(Map.Entry.<Video, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> videoService.toVideoResponse(e.getKey()))
                .collect(Collectors.toList());
    }

    private VideoFeatureVector buildVideoFeatureVector(Video video, int maxViewCount) {
        double popularity = maxViewCount > 0 ? (double) video.getViewCount() / maxViewCount : 0.0;
        return new VideoFeatureVector(
                video.getId(),
                video.getCefrLevel(),
                video.getTopic(),
                video.getDurationSec(),
                Map.of(), // phonemesDensity: requires pre-computed audio analysis
                cefrDifficulty(video.getCefrLevel()),
                popularity
        );
    }

    private double computeScore(UserFeatureVector user, VideoFeatureVector video, Set<UUID> seenIds) {
        double score = 0.0;

        // CEFR match (30%)
        int levelDiff = Math.abs(video.cefrLevel().ordinal() - user.cefrLevel().ordinal());
        double cefrMatch = levelDiff == 0 ? 1.0 : levelDiff == 1 ? 0.5 : 0.0;
        score += 0.30 * cefrMatch;

        // Topic diversity (25%)
        score += 0.25 * topicScore(video.topic(), user.recentTopics());

        // Weakness match (25%) — phoneme density not pre-computed, skipped
        if (!user.weakPhonemes().isEmpty() && !video.phonemesDensity().isEmpty()) {
            double overlap = user.weakPhonemes().stream()
                    .mapToDouble(p -> video.phonemesDensity().getOrDefault(p, 0.0))
                    .sum() / user.weakPhonemes().size();
            score += 0.25 * overlap;
        }

        // Popularity (10%)
        score += 0.10 * video.popularityScore();

        // Not seen yet (10%)
        score += 0.10 * (seenIds.contains(video.videoId()) ? 0.0 : 1.0);

        return score;
    }

    private double topicScore(String topic, List<String> recentTopics) {
        if (topic == null || recentTopics.isEmpty() || !recentTopics.contains(topic)) {
            return 1.0;
        }
        int size = recentTopics.size();
        // last 3 = "just watched"
        List<String> latest = recentTopics.subList(Math.max(0, size - 3), size);
        return latest.contains(topic) ? 0.3 : 0.5;
    }

    private double cefrDifficulty(CEFRLevel level) {
        return switch (level) {
            case A1 -> 0.2;
            case A2 -> 0.4;
            case B1 -> 0.6;
            case B2 -> 0.8;
            case C1 -> 0.9;
            case C2 -> 1.0;
        };
    }
}
