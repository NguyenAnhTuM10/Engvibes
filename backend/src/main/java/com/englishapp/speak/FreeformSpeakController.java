package com.englishapp.speak;

import com.englishapp.ai.LlmClient;
import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.retell.RateLimitService;
import com.englishapp.speak.dto.IeltsFeedback;
import com.englishapp.speak.dto.IeltsFeedbackResponse;
import com.englishapp.user.UserService;
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
    private final WhisperClient whisperClient;
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
            byte[] bytes = audio.getBytes();
            String filename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm";

            WhisperResult whisperResult = whisperClient.transcribe(bytes, filename, audio.getContentType());
            String transcript = whisperResult != null && whisperResult.getText() != null
                    ? whisperResult.getText().trim() : "";

            String prompt = buildPrompt(situation, question, vocab, collocations, transcript);
            String raw = llmClient.chatCompletion(SYSTEM_PROMPT, prompt);
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

    private String buildPrompt(String situation, String question, String vocab, String collocations, String transcript) {
        return """
                You are an IELTS Speaking examiner evaluating a candidate's spoken response.

                SITUATION: %s
                QUESTION: "%s"
                SUGGESTED VOCABULARY: %s
                SUGGESTED COLLOCATIONS: %s

                CANDIDATE'S TRANSCRIPT:
                "%s"

                Return ONLY this JSON structure (no markdown):
                {
                  "transcript": "<copy of the transcript above>",
                  "fluency": <0.0–9.0, one decimal, based on pace, hesitations, coherence>,
                  "grammar": <0.0–9.0, one decimal, accuracy and range of grammatical structures>,
                  "vocabulary": <0.0–9.0, one decimal, range, accuracy, and use of suggested words>,
                  "pronunciation": <0.0–9.0, one decimal, estimated from grammar/word choice patterns>,
                  "overall": <0.0–9.0, one decimal, weighted average>,
                  "feedback": "<2-3 sentences: one strength, one improvement tip specific to this situation>"
                }

                IELTS BAND DESCRIPTORS: 9=Expert, 8=Very Good, 7=Good, 6=Competent, 5=Modest, 4=Limited
                RULES:
                - overall = (fluency*0.25 + grammar*0.25 + vocabulary*0.25 + pronunciation*0.25), round to 1 decimal
                - If transcript is empty or "(no speech detected)": all scores 0, feedback encourages them to try
                - feedback must reference the specific situation, not be generic
                """.formatted(
                situation,
                question,
                vocab.isBlank() ? "none" : vocab,
                collocations.isBlank() ? "none" : collocations,
                transcript.isEmpty() ? "(no speech detected)" : transcript
        );
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        throw new RuntimeException("No JSON object found in model response: " + raw.substring(0, Math.min(200, raw.length())));
    }
}
