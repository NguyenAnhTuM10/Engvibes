package com.englishapp.shadow;

import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.shadow.dto.ShadowAttemptResponse;
import com.englishapp.shadow.dto.ShadowResultItem;
import com.englishapp.storage.StorageService;
import com.englishapp.user.UserService;
import com.englishapp.video.dto.SubtitleSegmentResponse;
import com.englishapp.video.subtitle.SubtitleSegment;
import com.englishapp.video.subtitle.SubtitleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionId}/shadow")
@RequiredArgsConstructor
public class ShadowController {

    private final SessionRepository sessionRepository;
    private final SubtitleService subtitleService;
    private final ShadowAttemptRepository shadowAttemptRepository;
    private final WhisperClient whisperClient;
    private final StorageService storageService;
    private final PhonemeDetectionService phonemeDetectionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    private static final int MAX_ATTEMPTS = 3;

    @GetMapping("/segments")
    public ApiResponse<List<SubtitleSegmentResponse>> getSegments(@PathVariable UUID sessionId) {
        LearningSession session = requireSession(sessionId, currentUserId());
        return ApiResponse.ok(subtitleService.getSegmentResponses(session.getVideoId()));
    }

    @PostMapping("/{segmentIdx}/attempt")
    public ApiResponse<ShadowAttemptResponse> submitAttempt(@PathVariable UUID sessionId,
                                                            @PathVariable int segmentIdx,
                                                            @RequestParam MultipartFile audio) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);

        List<SubtitleSegment> segments = subtitleService.getSegments(session.getVideoId());
        if (segmentIdx < 0 || segmentIdx >= segments.size()) {
            throw ApiException.badRequest("Invalid segment index");
        }
        SubtitleSegment segment = segments.get(segmentIdx);

        int existing = shadowAttemptRepository.countBySessionIdAndSegmentId(sessionId, segment.getId());
        if (existing >= MAX_ATTEMPTS) {
            throw ApiException.badRequest("Maximum " + MAX_ATTEMPTS + " attempts per segment");
        }
        int attemptNumber = existing + 1;

        String audioKey = String.format("sessions/%s/shadow-%d-%d.webm", sessionId, segmentIdx, attemptNumber);
        try {
            byte[] bytes = audio.getBytes();
            storageService.upload(recordingsBucket, audioKey,
                    new ByteArrayInputStream(bytes), bytes.length,
                    audio.getContentType() != null ? audio.getContentType() : "audio/webm");

            WhisperResult result = whisperClient.transcribe(bytes,
                    audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm",
                    audio.getContentType());

            String transcript = result != null && result.getText() != null ? result.getText().trim() : "";
            List<WordMatch> wordMatches = WordDiffUtil.diff(segment.getText(), transcript);
            double accuracy = WordDiffUtil.accuracy(wordMatches);
            List<String> weakPhonemes = phonemeDetectionService.detectWeakPhonemes(wordMatches);
            phonemeDetectionService.updateUserPhonemeStats(userId, wordMatches);

            ShadowAttempt attempt = ShadowAttempt.builder()
                    .sessionId(sessionId)
                    .segmentId(segment.getId())
                    .attemptNumber(attemptNumber)
                    .audioUrl(audioKey)
                    .transcript(transcript)
                    .wordDiff(objectMapper.writeValueAsString(wordMatches))
                    .accuracyScore(accuracy)
                    .weakPhonemesDetected(objectMapper.writeValueAsString(weakPhonemes))
                    .build();
            ShadowAttempt saved = shadowAttemptRepository.save(attempt);

            return ApiResponse.ok(ShadowAttemptResponse.builder()
                    .id(saved.getId())
                    .attemptNumber(attemptNumber)
                    .transcript(transcript)
                    .wordMatches(wordMatches)
                    .accuracy(accuracy)
                    .weakPhonemes(weakPhonemes)
                    .build());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Shadow attempt failed for session {}: {}", sessionId, e.getMessage());
            throw ApiException.badRequest("Failed to process audio: " + e.getMessage());
        }
    }

    @GetMapping("/result")
    public ApiResponse<List<ShadowResultItem>> getResult(@PathVariable UUID sessionId) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);

        List<SubtitleSegment> segments = subtitleService.getSegments(session.getVideoId());
        List<ShadowAttempt> allAttempts = shadowAttemptRepository.findBySessionIdOrderByCreatedAt(sessionId);

        Map<UUID, List<ShadowAttempt>> bySegment = allAttempts.stream()
                .collect(Collectors.groupingBy(ShadowAttempt::getSegmentId));

        List<ShadowResultItem> result = new ArrayList<>();
        for (SubtitleSegment seg : segments) {
            List<ShadowAttempt> attempts = bySegment.getOrDefault(seg.getId(), List.of());
            ShadowAttempt best = attempts.stream()
                    .max(Comparator.comparingDouble(ShadowAttempt::getAccuracyScore))
                    .orElse(null);

            ShadowAttemptResponse bestResponse = null;
            if (best != null) {
                bestResponse = ShadowAttemptResponse.builder()
                        .id(best.getId())
                        .attemptNumber(best.getAttemptNumber())
                        .transcript(best.getTranscript())
                        .wordMatches(parseWordMatches(best.getWordDiff()))
                        .accuracy(best.getAccuracyScore())
                        .weakPhonemes(parsePhonemes(best.getWeakPhonemesDetected()))
                        .build();
            }

            result.add(ShadowResultItem.builder()
                    .segmentId(seg.getId())
                    .orderIndex(seg.getOrderIndex())
                    .segmentText(seg.getText())
                    .totalAttempts(attempts.size())
                    .bestAccuracy(best != null ? best.getAccuracyScore() : 0)
                    .bestAttempt(bestResponse)
                    .build());
        }
        return ApiResponse.ok(result);
    }

    private List<WordMatch> parseWordMatches(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<WordMatch>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parsePhonemes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private LearningSession requireSession(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return session;
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
