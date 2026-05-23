package com.englishapp.speak;

import com.englishapp.ai.LlmClient;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.retell.RateLimitService;
import com.englishapp.speak.dto.SpeakFeedback;
import com.englishapp.speak.dto.SpeakFeedbackResponse;
import com.englishapp.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Speaking Practice", description = "Freeform speaking practice with AI feedback")
@Slf4j
@RestController
@RequestMapping("/api/speak")
@RequiredArgsConstructor
public class FreeformSpeakController {

    private final LlmClient llmClient;
    private final RateLimitService rateLimitService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final int DAILY_LIMIT = 30;
    private static final String SYSTEM_PROMPT =
            "You are an English speaking coach. Listen to the audio and return only valid JSON, no markdown code blocks.";

    @PostMapping("/freeform")
    public ApiResponse<SpeakFeedbackResponse> assessFreeform(
            @RequestParam MultipartFile audio,
            @RequestParam String situation,
            @RequestParam String question,
            @RequestParam(defaultValue = "") String vocab,
            @RequestParam(defaultValue = "") String collocations
    ) {
        UUID userId = userService.getCurrentUser().getId();
        if (!rateLimitService.tryAcquire(userId, "speak-freeform", DAILY_LIMIT)) {
            throw ApiException.badRequest("Daily limit reached (30/day). Try again tomorrow.");
        }

        try {
            byte[] bytes = audio.getBytes();
            String audioFormat = resolveFormat(audio.getContentType(), audio.getOriginalFilename());

            String prompt = buildPrompt(situation, question, vocab, collocations);
            String raw = llmClient.chatCompletionWithAudio(SYSTEM_PROMPT, prompt, bytes, audioFormat);
            SpeakFeedback feedback = objectMapper.readValue(extractJson(raw), SpeakFeedback.class);

            log.info("Freeform speak for user {} — situation='{}' score={}", userId, situation, feedback.overallScore());

            List<String> vocabUsed = feedback.vocabFromVideoUsed() != null
                    ? feedback.vocabFromVideoUsed().stream().map(SpeakFeedback.VocabUsed::word).toList()
                    : List.of();
            List<SpeakFeedbackResponse.GrammarIssue> grammarIssues = feedback.grammarIssues() != null
                    ? feedback.grammarIssues().stream()
                        .map(g -> new SpeakFeedbackResponse.GrammarIssue(g.errorQuote(), g.correction(), g.briefExplain()))
                        .toList()
                    : List.of();

            return ApiResponse.ok(SpeakFeedbackResponse.builder()
                    .score(feedback.overallScore())
                    .fluencyScore(feedback.fluencyScore())
                    .grammarScore(feedback.grammarScore())
                    .vocabVarietyScore(feedback.vocabVarietyScore())
                    .transcript(feedback.transcript() != null ? feedback.transcript() : "")
                    .vocabFromVideoUsed(vocabUsed)
                    .grammarIssues(grammarIssues)
                    .positiveNotes(feedback.positiveNotes() != null ? feedback.positiveNotes() : List.of())
                    .improvementTips(feedback.improvementTips() != null ? feedback.improvementTips() : List.of())
                    .modelAnswer(feedback.modelAnswer())
                    .build());

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Freeform speak failed: {}", e.getMessage());
            throw ApiException.badRequest("Failed to assess speaking: " + e.getMessage());
        }
    }

    private String buildPrompt(String situation, String question, String vocab, String collocations) {
        return """
                You are an English speaking coach evaluating a B1-B2 learner's spoken response.
                Listen carefully to the audio — pay attention to pronunciation, intonation, hesitations, and natural flow.

                SITUATION: %s
                QUESTION ASKED: "%s"
                SUGGESTED VOCABULARY: %s
                SUGGESTED COLLOCATIONS: %s

                Return a JSON object (no markdown) with this exact structure:
                {
                  "transcript": "<transcribe what the user said verbatim>",
                  "fluency_score": <int 0-100, based on pace, hesitations, natural flow>,
                  "grammar_score": <int 0-100>,
                  "vocab_variety_score": <int 0-100>,
                  "vocab_from_video_used": [{"word": "<suggested word the user used>", "in_sentence": "<the sentence>"}],
                  "vocab_from_video_missed": ["<suggested words the user did not use>"],
                  "grammar_issues": [{"error_quote": "<wrong phrase>", "correction": "<corrected>", "brief_explain": "<why>"}],
                  "positive_notes": ["<specific encouraging observation about their speech>"],
                  "improvement_tips": ["<concrete actionable tip for this situation>"],
                  "model_answer": "<natural B1-B2 model answer using the suggested vocabulary, 3-4 sentences>",
                  "overall_score": <int 0-100>
                }

                RULES:
                - Be encouraging and constructive
                - Max 3 grammar issues
                - overall_score = 0.4 * fluency + 0.35 * grammar + 0.25 * vocab_variety
                - fluency_score must reflect actual spoken delivery (pauses, filler words, pace) — not just content
                - If audio is silent or under 3 seconds, give low scores and encourage them to try speaking
                - improvement_tips must be specific to this situation, not generic advice
                """.formatted(
                situation,
                question,
                vocab.isBlank() ? "none specified" : vocab,
                collocations.isBlank() ? "none specified" : collocations
        );
    }

    private String resolveFormat(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.contains("webm")) return "webm";
            if (contentType.contains("mp4")) return "mp4";
            if (contentType.contains("mpeg")) return "mpeg";
            if (contentType.contains("wav")) return "wav";
            if (contentType.contains("ogg")) return "ogg";
        }
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) return filename.substring(dot + 1).toLowerCase();
        }
        return "webm";
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
}
