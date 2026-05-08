package com.englishapp.recommend;

import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.flashcard.FlashcardMapper;
import com.englishapp.flashcard.UserCard;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.recommend.dto.DailyChallengeResponse;
import com.englishapp.shadow.UserPhonemeStatsRepository;
import com.englishapp.user.CEFRLevel;
import com.englishapp.user.UserService;
import com.englishapp.video.dto.VideoResponse;
import com.englishapp.video.subtitle.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final ContentBasedRecommender recommender;
    private final CardRepository cardRepository;
    private final FlashcardMapper flashcardMapper;
    private final UserPhonemeStatsRepository phonemeStatsRepository;
    private final SubtitleRepository subtitleRepository;
    private final UserService userService;

    @GetMapping("/videos")
    public ApiResponse<List<VideoResponse>> recommendVideos(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(recommender.recommend(currentUserId(), limit));
    }

    @GetMapping("/vocab-priority")
    public ApiResponse<List<CardResponse>> vocabPriority(
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = currentUserId();
        CEFRLevel userCefr = userService.getCurrentUser().getCefrLevel();
        List<String> weakPhonemes = buildWeakPhonemes(userId);
        Instant now = Instant.now();

        List<CardResponse> sorted = cardRepository.findDueByUserId(userId, now).stream()
                .sorted(Comparator.comparingDouble((UserCard c) ->
                        -computeVocabScore(c, weakPhonemes, userCefr, now)))
                .limit(limit)
                .map(flashcardMapper::toCardResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(sorted);
    }

    @GetMapping("/daily-challenge")
    public ApiResponse<DailyChallengeResponse> dailyChallenge() {
        UUID userId = currentUserId();
        CEFRLevel userCefr = userService.getCurrentUser().getCefrLevel();
        List<String> weakPhonemes = buildWeakPhonemes(userId);
        Instant now = Instant.now();

        List<VideoResponse> topVideos = recommender.recommend(userId, 1);
        VideoResponse recommendedVideo = topVideos.isEmpty() ? null : topVideos.get(0);

        List<CardResponse> vocabsToReview = cardRepository.findDueByUserId(userId, now).stream()
                .sorted(Comparator.comparingDouble((UserCard c) ->
                        -computeVocabScore(c, weakPhonemes, userCefr, now)))
                .limit(5)
                .map(flashcardMapper::toCardResponse)
                .collect(Collectors.toList());

        String randomPhrase = subtitleRepository.findRandomPublishedPhrase()
                .map(s -> s.getText())
                .orElse(null);

        return ApiResponse.ok(DailyChallengeResponse.builder()
                .recommendedVideo(recommendedVideo)
                .vocabsToReview(vocabsToReview)
                .randomPhrase(randomPhrase)
                .build());
    }

    private double computeVocabScore(UserCard card, List<String> weakPhonemes, CEFRLevel userCefr, Instant now) {
        double score = 1.0;

        // Phoneme overlap (30%)
        String phonemes = card.getVocab().getPhonemes();
        if (phonemes != null && !weakPhonemes.isEmpty()) {
            String phonemesUpper = phonemes.toUpperCase();
            long matches = weakPhonemes.stream()
                    .filter(p -> phonemesUpper.contains(p.toUpperCase()))
                    .count();
            score += 0.3 * ((double) matches / weakPhonemes.size());
        }

        // CEFR match (20%)
        if (userCefr != null && userCefr.equals(card.getVocab().getCefrLevel())) {
            score += 0.2;
        }

        // Days overdue (10%)
        if (card.getNextReview() != null && card.getNextReview().isBefore(now)) {
            long daysOverdue = ChronoUnit.DAYS.between(card.getNextReview(), now);
            score += 0.1 * Math.min(daysOverdue, 30);
        }

        return score;
    }

    private List<String> buildWeakPhonemes(UUID userId) {
        return phonemeStatsRepository.findByIdUserId(userId).stream()
                .filter(s -> s.getTotalAttempts() >= 5)
                .sorted(Comparator.comparingDouble(s -> -((double) s.getErrors() / s.getTotalAttempts())))
                .limit(5)
                .map(s -> s.getId().getPhoneme())
                .collect(Collectors.toList());
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
