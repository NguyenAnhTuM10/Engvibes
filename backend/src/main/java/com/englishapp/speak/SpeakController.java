package com.englishapp.speak;

import com.englishapp.ai.LlmClient;
import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.retell.RateLimitService;
import com.englishapp.session.LearningSession;
import com.englishapp.session.SessionRepository;
import com.englishapp.speak.dto.SpeakFeedback;
import com.englishapp.speak.dto.SpeakingQuestionResponse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionId}/speak")
@RequiredArgsConstructor
public class SpeakController {

    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final SpeakAttemptRepository speakAttemptRepository;
    private final WhisperClient whisperClient;
    private final LlmClient llmClient;
    private final StorageService storageService;
    private final RateLimitService rateLimitService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    private static final int DAILY_SPEAK_LIMIT = 50;
    private static final String SYSTEM_PROMPT =
            "You are an English speaking coach. Return only valid JSON, no markdown code blocks.";

    @GetMapping("/question")
    public ApiResponse<SpeakingQuestionResponse> getQuestion(@PathVariable UUID sessionId) {
        LearningSession session = requireSession(sessionId, currentUserId());
        Video video = loadVideo(session.getVideoId());

        List<WarmupWord> vocab = parseWarmupWords(video.getWarmupWordsJson());
        Map<String, List<String>> collocations = parseCollocations(video.getCollocationsJson());
        List<String> flatCollocations = collocations.values().stream()
                .flatMap(List::stream).limit(6).toList();

        return ApiResponse.ok(SpeakingQuestionResponse.builder()
                .question(video.getSpeakingQuestion() != null
                        ? video.getSpeakingQuestion()
                        : "What do you think about the topic discussed in this video?")
                .suggestedVocab(vocab)
                .suggestedCollocations(flatCollocations)
                .build());
    }

    @PostMapping("/attempt")
    public ApiResponse<SpeakFeedback> submitAttempt(@PathVariable UUID sessionId,
                                                    @RequestParam MultipartFile audio) {
        UUID userId = currentUserId();
        if (!rateLimitService.tryAcquire(userId, "speak", DAILY_SPEAK_LIMIT)) {
            throw ApiException.badRequest("Daily speaking limit reached (50/day). Try again tomorrow.");
        }

        LearningSession session = requireSession(sessionId, userId);
        Video video = loadVideo(session.getVideoId());
        int attemptNumber = speakAttemptRepository.findBySessionIdOrderByCreatedAt(sessionId).size() + 1;

        String audioKey = String.format("sessions/%s/speak-%d.webm", sessionId, attemptNumber);
        try {
            byte[] bytes = audio.getBytes();
            storageService.upload(recordingsBucket, audioKey,
                    new ByteArrayInputStream(bytes), bytes.length,
                    audio.getContentType() != null ? audio.getContentType() : "audio/webm");

            WhisperResult whisperResult = whisperClient.transcribe(bytes,
                    audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm",
                    audio.getContentType());

            String transcript = whisperResult != null && whisperResult.getText() != null
                    ? whisperResult.getText().trim() : "";

            String prompt = buildSpeakPrompt(video, transcript);
            String raw = llmClient.chatCompletion(SYSTEM_PROMPT, prompt);
            SpeakFeedback feedback = objectMapper.readValue(extractJson(raw), SpeakFeedback.class);

            SpeakAttempt attempt = SpeakAttempt.builder()
                    .sessionId(sessionId)
                    .audioUrl(audioKey)
                    .transcript(transcript)
                    .aiFeedback(objectMapper.writeValueAsString(feedback))
                    .overallScore(feedback.overallScore())
                    .build();
            speakAttemptRepository.save(attempt);

            log.info("Speak attempt {} for session {} — score={}", attemptNumber, sessionId, feedback.overallScore());
            return ApiResponse.ok(feedback);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Speak attempt failed for session {}: {}", sessionId, e.getMessage());
            throw ApiException.badRequest("Failed to process speaking: " + e.getMessage());
        }
    }

    private String buildSpeakPrompt(Video video, String transcript) {
        List<WarmupWord> vocab = parseWarmupWords(video.getWarmupWordsJson());
        String vocabList = vocab.stream().map(WarmupWord::word).collect(Collectors.joining(", "));
        Map<String, List<String>> collocations = parseCollocations(video.getCollocationsJson());
        String collocationsList = collocations.values().stream()
                .flatMap(List::stream).limit(6).collect(Collectors.joining(", "));

        return """
                You are an English speaking coach evaluating an open-ended response from a B1 learner.

                QUESTION ASKED: "%s"
                SUGGESTED VOCAB FROM VIDEO: %s
                SUGGESTED COLLOCATIONS: %s

                USER'S RESPONSE:
                - Transcript: "%s"

                Return JSON (no markdown):
                {
                  "fluency_score": <0-100>,
                  "grammar_score": <0-100>,
                  "vocab_variety_score": <0-100>,
                  "vocab_from_video_used": [{"word": "<>", "in_sentence": "<>"}],
                  "vocab_from_video_missed": ["<>"],
                  "grammar_issues": [{"error_quote": "<>", "correction": "<>", "brief_explain": "<>"}],
                  "positive_notes": ["..."],
                  "improvement_tips": ["..."],
                  "model_answer": "<B1 model answer using suggested vocab>",
                  "overall_score": <0-100>
                }
                Max 3 grammar issues. Be encouraging.
                """.formatted(
                video.getSpeakingQuestion() != null ? video.getSpeakingQuestion() : "Share your thoughts on the topic.",
                vocabList.isEmpty() ? "None" : vocabList,
                collocationsList.isEmpty() ? "None" : collocationsList,
                transcript.isEmpty() ? "(no speech detected)" : transcript
        );
    }

    private List<WarmupWord> parseWarmupWords(String json) {
        try { return objectMapper.readValue(json, new TypeReference<List<WarmupWord>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private Map<String, List<String>> parseCollocations(String json) {
        try { return objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {}); }
        catch (Exception e) { return Map.of(); }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) return trimmed.substring(start, end).trim();
        }
        return trimmed;
    }

    private LearningSession requireSession(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return session;
    }

    private Video loadVideo(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
