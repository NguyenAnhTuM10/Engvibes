package com.englishapp.game;

import com.englishapp.common.ApiResponse;
import com.englishapp.game.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sm2/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService service;

    // ── 1. Multiple Choice ───────────────────────────────────────────────

    /** GET /api/sm2/games/multiple-choice?deck_id=&count=10 */
    @GetMapping("/multiple-choice")
    public ApiResponse<List<MultipleChoiceQuestion>> multipleChoice(
            @RequestParam UUID deck_id,
            @RequestParam(defaultValue = "10") int count) {
        return ApiResponse.ok(service.generateMultipleChoice(deck_id, count));
    }

    /**
     * POST /api/sm2/games/answer
     * { "cardId": "...", "selected": "nghĩa user chọn" }
     * Chấm đúng/sai, cập nhật SM-2, trả kết quả.
     */
    @PostMapping("/answer")
    public ApiResponse<AnswerResult> answer(@RequestBody AnswerRequest req) {
        return ApiResponse.ok(service.checkAnswer(req.cardId(), req.selected()));
    }

    // ── 2. Matching ──────────────────────────────────────────────────────

    /** GET /api/sm2/games/matching?deck_id=&count=6 */
    @GetMapping("/matching")
    public ApiResponse<MatchingGame> matching(
            @RequestParam UUID deck_id,
            @RequestParam(defaultValue = "6") int count) {
        return ApiResponse.ok(service.generateMatching(deck_id, count));
    }

    /**
     * POST /api/sm2/games/matching/check
     * { "pairs": [{"cardId":"...", "matchedBack":"..."}] }
     */
    @PostMapping("/matching/check")
    public ApiResponse<MatchingCheckResult> checkMatching(
            @RequestBody MatchingCheckRequest req) {
        return ApiResponse.ok(service.checkMatching(req.pairs()));
    }

    // ── 3. Typing Recall ─────────────────────────────────────────────────

    /** GET /api/sm2/games/typing?deck_id=&count=10 */
    @GetMapping("/typing")
    public ApiResponse<List<TypingQuestion>> typing(
            @RequestParam UUID deck_id,
            @RequestParam(defaultValue = "10") int count) {
        return ApiResponse.ok(service.generateTyping(deck_id, count));
    }

    /**
     * POST /api/sm2/games/typing/check
     * { "cardId": "...", "typed": "user input" }
     * Levenshtein ≤ 1 → đúng. Cập nhật SM-2.
     */
    @PostMapping("/typing/check")
    public ApiResponse<TypingCheckResult> checkTyping(
            @RequestBody TypingCheckRequest req) {
        return ApiResponse.ok(service.checkTyping(req.cardId(), req.typed()));
    }
}
