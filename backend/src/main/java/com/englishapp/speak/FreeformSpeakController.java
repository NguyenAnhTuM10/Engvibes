package com.englishapp.speak;

import com.englishapp.ai.LlmClient;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.retell.RateLimitService;
import com.englishapp.speak.dto.IeltsFeedback;
import com.englishapp.speak.dto.IeltsFeedbackResponse;
import com.englishapp.user.UserService;
import com.englishapp.video.FfmpegService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Speaking Practice", description = "Freeform speaking practice with GPT Audio evaluation")
@Slf4j
@RestController
@RequestMapping("/api/speak")
@RequiredArgsConstructor
public class FreeformSpeakController {

    private final LlmClient llmClient;
    private final FfmpegService ffmpegService;
    private final RateLimitService rateLimitService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final int DAILY_LIMIT = 30;

    private static final String SYSTEM_PROMPT =
            "You are an IELTS Speaking examiner. Listen to the audio carefully. " +
            "Return ONLY a valid JSON object — no markdown, no explanation, no extra text.";

    @PostMapping("/freeform")
    public ApiResponse<IeltsFeedbackResponse> assessFreeform(
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
            byte[] rawBytes = audio.getBytes();
            String originalFormat = resolveFormat(audio.getContentType(), audio.getOriginalFilename());

            // gpt-audio-mini only accepts wav and mp3 — convert if needed
            byte[] bytes = "wav".equals(originalFormat) || "mp3".equals(originalFormat)
                    ? rawBytes
                    : ffmpegService.convertToWav(rawBytes, originalFormat);
            String audioFormat = "wav".equals(originalFormat) || "mp3".equals(originalFormat)
                    ? originalFormat : "wav";

            String prompt = buildPrompt(situation, question, vocab, collocations);
            String raw = llmClient.chatCompletionWithAudio(SYSTEM_PROMPT, prompt, bytes, audioFormat);
            IeltsFeedback feedback = objectMapper.readValue(extractJson(raw), IeltsFeedback.class);

            log.info("Speaking eval — user={} situation='{}' overall={}", userId, situation, feedback.overall());

            return ApiResponse.ok(IeltsFeedbackResponse.builder()
                    .transcript(feedback.transcript() != null ? feedback.transcript() : "")
                    .fluency(feedback.fluency())
                    .grammar(feedback.grammar())
                    .vocabulary(feedback.vocabulary())
                    .pronunciation(feedback.pronunciation())
                    .overall(feedback.overall())
                    .feedback(feedback.feedback() != null ? feedback.feedback() : "")
                    .build());

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Speaking eval failed: {}", e.getMessage());
            throw ApiException.badRequest("Failed to assess speaking: " + e.getMessage());
        }
    }

    private String buildPrompt(String situation, String question, String vocab, String collocations) {
        return """
                You are an IELTS Speaking examiner evaluating a candidate's response.
                Listen to the audio carefully — assess pronunciation, fluency, grammar, and vocabulary.

                SITUATION: %s
                QUESTION: "%s"
                SUGGESTED VOCABULARY: %s
                SUGGESTED COLLOCATIONS: %s

                Return ONLY this JSON structure (no markdown):
                {
                  "transcript": "<verbatim transcription of what the candidate said>",
                  "fluency": <0.0–9.0, one decimal, based on pace, hesitations, coherence>,
                  "grammar": <0.0–9.0, one decimal, accuracy and range of grammatical structures>,
                  "vocabulary": <0.0–9.0, one decimal, range, accuracy, and use of suggested words>,
                  "pronunciation": <0.0–9.0, one decimal, clarity, intonation, stress>,
                  "overall": <0.0–9.0, one decimal, weighted average>,
                  "feedback": "<2-3 sentences: one strength, one improvement tip specific to this situation>"
                }

                IELTS BAND DESCRIPTORS (use as reference):
                9 = Expert, 8 = Very Good, 7 = Good, 6 = Competent, 5 = Modest, 4 = Limited, below 4 = struggling

                RULES:
                - overall = (fluency*0.25 + grammar*0.25 + vocabulary*0.25 + pronunciation*0.25), round to 1 decimal
                - If audio is silent or under 3 seconds: all scores 0, feedback encourages them to try speaking
                - feedback must reference the specific situation, not be generic
                """.formatted(
                situation,
                question,
                vocab.isBlank() ? "none" : vocab,
                collocations.isBlank() ? "none" : collocations
        );
    }

    private String resolveFormat(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.contains("webm")) return "webm";
            if (contentType.contains("mp4"))  return "mp4";
            if (contentType.contains("mpeg")) return "mpeg";
            if (contentType.contains("wav"))  return "wav";
            if (contentType.contains("ogg"))  return "ogg";
        }
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) return filename.substring(dot + 1).toLowerCase();
        }
        return "webm";
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        throw new RuntimeException("No JSON object found in model response: " + raw.substring(0, Math.min(200, raw.length())));
    }
}
