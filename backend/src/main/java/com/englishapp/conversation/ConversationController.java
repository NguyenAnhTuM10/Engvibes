package com.englishapp.conversation;

import com.englishapp.ai.LlmClient;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.conversation.dto.*;
import com.englishapp.user.UserService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Conversation Practice", description = "Real-time AI roleplay conversation with keyword hints")
@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final LlmClient llmClient;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final String REVIEW_SYSTEM_PROMPT =
            "You are an English language coach. Return only valid JSON, no markdown.";

    // ── Realtime endpoints ─────────────────────────────────────────────────────

    @PostMapping("/realtime-token")
    public ApiResponse<RealtimeTokenResponse> getRealtimeToken(
            @Valid @RequestBody RealtimeTokenRequest req) {
        ConversationScenario scenario = parseScenario(req.scenarioId());
        String instructions = buildRealtimeInstructions(scenario);
        String token = llmClient.getRealtimeToken(instructions, "alloy");
        return ApiResponse.ok(new RealtimeTokenResponse(
                token,
                llmClient.getRealtimeModel(),
                req.scenarioId(),
                scenario.displayName,
                scenario.aiRole,
                scenario.userGoal,
                scenario.openingLine));
    }

    @PostMapping("/realtime-review")
    public ApiResponse<ConversationReviewResponse> realtimeReview(
            @RequestBody ConversationReviewRequest req) {
        try {
            String prompt = buildReviewPrompt(req);
            String raw = llmClient.chatCompletion(REVIEW_SYSTEM_PROMPT, prompt);
            ReviewJson parsed = objectMapper.readValue(extractJson(raw), ReviewJson.class);

            List<ConversationReviewResponse.GrammarNote> notes = parsed.grammarNotes() != null
                    ? parsed.grammarNotes().stream()
                        .map(n -> new ConversationReviewResponse.GrammarNote(n.error(), n.correction()))
                        .toList()
                    : List.of();

            return ApiResponse.ok(ConversationReviewResponse.builder()
                    .fluency(parsed.fluency())
                    .grammar(parsed.grammar())
                    .vocabulary(parsed.vocabulary())
                    .overall(parsed.overall())
                    .strengths(parsed.strengths())
                    .improvements(parsed.improvements())
                    .encouragement(parsed.encouragement())
                    .grammarNotes(notes)
                    .build());
        } catch (Exception e) {
            log.error("Conversation review failed: {}", e.getMessage());
            throw ApiException.badRequest("Failed to review conversation: " + e.getMessage());
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private String buildRealtimeInstructions(ConversationScenario scenario) {
        return """
                You are %s, helping an English learner (B1-B2) practice real conversation.

                SCENARIO: %s
                LEARNER'S GOAL: %s

                RULES:
                - Speak naturally as your character — do NOT return JSON
                - Keep each response to 1-3 sentences
                - Be warm, encouraging, and supportive
                - Gently rephrase major grammar mistakes naturally in your reply (don't lecture)
                - Start the conversation with: "%s"
                - After 5-6 user turns, wrap up the conversation naturally
                """.formatted(
                scenario.aiRole, scenario.description, scenario.userGoal, scenario.openingLine);
    }

    private String buildReviewPrompt(ConversationReviewRequest req) {
        ConversationScenario scenario = parseScenario(req.scenarioId());
        String userLines = req.turns().stream()
                .filter(t -> "user".equals(t.role()) && t.text() != null && !t.text().isBlank())
                .map(t -> "- " + t.text())
                .reduce("", (a, b) -> a + "\n" + b);

        return """
                Evaluate this English conversation practice session.
                SCENARIO: %s | DURATION: %d seconds

                USER'S MESSAGES:
                %s

                Return JSON (no markdown):
                {
                  "fluency": <0.0-9.0, one decimal — pace, hesitation, coherence>,
                  "grammar": <0.0-9.0, one decimal — accuracy and range>,
                  "vocabulary": <0.0-9.0, one decimal — range and appropriateness>,
                  "overall": <0.0-9.0, one decimal — weighted average>,
                  "strengths": "<1-2 sentences on what they did well>",
                  "improvements": "<1-2 sentences most impactful improvement tip>",
                  "encouragement": "<1 warm encouraging sentence>",
                  "grammar_notes": [{"error": "<wrong>", "correction": "<correct>"}]
                }
                Max 3 grammar_notes. If user said very little, give low scores and encourage them.
                """.formatted(scenario.displayName, req.durationSec(), userLines.isBlank() ? "(no speech)" : userLines);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReviewJson(
            double fluency, double grammar, double vocabulary, double overall,
            String strengths, String improvements, String encouragement,
            @JsonProperty("grammar_notes") List<GrammarNoteJson> grammarNotes) {
        record GrammarNoteJson(String error, String correction) {}
    }

    private ConversationScenario parseScenario(String id) {
        try { return ConversationScenario.valueOf(id); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown scenario: " + id); }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        return raw.trim();
    }

    @GetMapping("/scenarios")
    public ApiResponse<List<ScenarioResponse>> getScenarios() {
        return ApiResponse.ok(conversationService.getScenarios());
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationSessionResponse> startSession(
            @Valid @RequestBody StartConversationRequest request) {
        return ApiResponse.ok(conversationService.startSession(currentUserId(), request.scenarioId()));
    }

    @PostMapping("/{sessionId}/turn")
    public ApiResponse<ConversationTurnResponse> processTurn(
            @PathVariable UUID sessionId,
            @RequestParam MultipartFile audio) {
        return ApiResponse.ok(conversationService.processTurn(sessionId, currentUserId(), audio));
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponse<ConversationEndResponse> endSession(@PathVariable UUID sessionId) {
        return ApiResponse.ok(conversationService.endSession(sessionId, currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
