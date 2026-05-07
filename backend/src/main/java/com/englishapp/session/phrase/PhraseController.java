package com.englishapp.session.phrase;

import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.session.phrase.dto.AttemptResultResponse;
import com.englishapp.session.phrase.dto.PhraseItem;
import com.englishapp.shadow.WordDiffUtil;
import com.englishapp.shadow.WordMatch;
import com.englishapp.storage.StorageService;
import com.englishapp.user.UserService;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionId}/phrases")
@RequiredArgsConstructor
public class PhraseController {

    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final PhraseAttemptRepository phraseAttemptRepository;
    private final WhisperClient whisperClient;
    private final StorageService storageService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    @GetMapping
    public ApiResponse<List<PhraseItem>> listPhrases(@PathVariable UUID sessionId) {
        LearningSession session = requireSession(sessionId, currentUserId());
        Video video = videoRepository.findById(session.getVideoId())
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        return ApiResponse.ok(extractPhrases(video.getCollocationsJson()));
    }

    @PostMapping("/{idx}/attempt")
    public ApiResponse<AttemptResultResponse> submitAttempt(@PathVariable UUID sessionId,
                                                            @PathVariable int idx,
                                                            @RequestParam MultipartFile audio) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);
        Video video = videoRepository.findById(session.getVideoId())
                .orElseThrow(() -> ApiException.notFound("Video not found"));

        List<PhraseItem> phrases = extractPhrases(video.getCollocationsJson());
        if (idx < 0 || idx >= phrases.size()) {
            throw ApiException.badRequest("Invalid phrase index");
        }
        String targetPhrase = phrases.get(idx).getPhrase();

        // Upload audio
        String audioKey = String.format("sessions/%s/phrase-%d.webm", sessionId, idx);
        try {
            byte[] bytes = audio.getBytes();
            storageService.upload(recordingsBucket, audioKey,
                    new ByteArrayInputStream(bytes), bytes.length,
                    audio.getContentType() != null ? audio.getContentType() : "audio/webm");

            // Transcribe
            WhisperResult result = whisperClient.transcribe(bytes,
                    audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm",
                    audio.getContentType());

            String transcript = result != null && result.getText() != null ? result.getText().trim() : "";
            List<WordMatch> wordMatches = WordDiffUtil.diff(targetPhrase, transcript);
            double accuracy = WordDiffUtil.accuracy(wordMatches);

            PhraseAttempt attempt = PhraseAttempt.builder()
                    .sessionId(sessionId)
                    .phraseIdx(idx)
                    .audioUrl(audioKey)
                    .transcript(transcript)
                    .accuracyScore(accuracy)
                    .build();
            PhraseAttempt saved = phraseAttemptRepository.save(attempt);

            return ApiResponse.ok(AttemptResultResponse.builder()
                    .id(saved.getId())
                    .transcript(transcript)
                    .wordMatches(wordMatches)
                    .accuracy(accuracy)
                    .build());
        } catch (Exception e) {
            log.error("Phrase attempt failed for session {}: {}", sessionId, e.getMessage());
            throw ApiException.badRequest("Failed to process audio: " + e.getMessage());
        }
    }

    private List<PhraseItem> extractPhrases(String collocationsJson) {
        try {
            Map<String, List<String>> collocations = objectMapper.readValue(
                    collocationsJson, new TypeReference<>() {});
            List<PhraseItem> result = new ArrayList<>();
            int idx = 0;
            for (Map.Entry<String, List<String>> entry : new TreeMap<>(collocations).entrySet()) {
                for (String phrase : entry.getValue()) {
                    result.add(PhraseItem.builder()
                            .idx(idx++)
                            .phrase(phrase)
                            .keyword(entry.getKey())
                            .build());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse collocations JSON: {}", e.getMessage());
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
