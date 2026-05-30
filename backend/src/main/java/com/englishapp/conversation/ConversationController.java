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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Conversation Practice", description = "Real-time AI roleplay conversation with keyword hints")
@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationRealtimeService realtimeService;
    private final LlmClient llmClient;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final String REVIEW_SYSTEM_PROMPT =
            "You are an English language coach. Return only valid JSON, no markdown.";

    // ── Realtime endpoints ─────────────────────────────────────────────────────
    // Realtime conversation chạy qua WS proxy (/ws/conversation). REST chỉ còn:
    // scenarios (list), realtime-review (chấm), history + detail (xem lại).

    /**
     * T2.2 — Chấm review dựa trên transcript SERVER-OBSERVED, không nhận từ client.
     * Yêu cầu sessionId; load transcript từ DB (authoritative), chấm, rồi lưu vào session.
     */
    @PostMapping("/realtime-review")
    public ApiResponse<ConversationReviewResponse> realtimeReview(
            @RequestBody ConversationReviewRequest req) {
        if (req.sessionId() == null) {
            throw ApiException.badRequest("sessionId is required for review");
        }
        UUID userId = currentUserId();
        ConversationSession session;
        try {
            session = realtimeService.getOwnedSession(req.sessionId(), userId);
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid session: " + e.getMessage());
        }

        try {
            // Transcript CHÍNH THỨC từ server-observed turns — client KHÔNG can thiệp
            List<String> userLines = realtimeService.loadUserTranscripts(req.sessionId());
            ConversationScenario scenario = parseScenario(session.getScenarioId());

            String prompt = buildReviewPrompt(scenario, userLines, req.durationSec());
            String raw = llmClient.chatCompletion(REVIEW_SYSTEM_PROMPT, prompt);
            ReviewJson parsed = objectMapper.readValue(extractJson(raw), ReviewJson.class);

            List<ConversationReviewResponse.GrammarNote> notes = parsed.grammarNotes() != null
                    ? parsed.grammarNotes().stream()
                        .map(n -> new ConversationReviewResponse.GrammarNote(n.error(), n.correction()))
                        .toList()
                    : List.of();

            ConversationReviewResponse response = ConversationReviewResponse.builder()
                    .fluency(parsed.fluency())
                    .grammar(parsed.grammar())
                    .vocabulary(parsed.vocabulary())
                    .overall(parsed.overall())
                    .strengths(parsed.strengths())
                    .improvements(parsed.improvements())
                    .encouragement(parsed.encouragement())
                    .grammarNotes(notes)
                    .build();

            // T2.2 — lưu review vào session để user xem lại sau
            try {
                realtimeService.saveReview(req.sessionId(), objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                log.warn("Failed to persist review for session {}: {}", req.sessionId(), e.getMessage());
            }

            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("Conversation review failed: {}", e.getMessage());
            throw ApiException.badRequest("Failed to review conversation: " + e.getMessage());
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private String buildReviewPrompt(ConversationScenario scenario, List<String> userLines, int durationSec) {
        String lines = userLines.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> "- " + t)
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
                """.formatted(scenario.displayName, durationSec, lines.isBlank() ? "(no speech)" : lines);
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

    /** T2.2 — Lịch sử session realtime của user (mới nhất trước). */
    @GetMapping("/history")
    public ApiResponse<List<ConversationSession>> history() {
        return ApiResponse.ok(realtimeService.getSessionHistory(currentUserId()));
    }

    /** T2.2 — Chi tiết 1 session: turns server-observed + review (summary JSON). */
    @GetMapping("/{sessionId}/detail")
    public ApiResponse<ConversationSession> sessionDetail(@PathVariable UUID sessionId) {
        return ApiResponse.ok(realtimeService.getOwnedSession(sessionId, currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
