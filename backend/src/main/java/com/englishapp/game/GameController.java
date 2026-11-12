package com.englishapp.game;

import com.englishapp.common.ApiResponse;
import com.englishapp.game.dto.*;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Vocabulary Games", description = "Multiple choice, matching and typing recall on a flashcard deck")
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService service;
    private final UserService userService;

    // ── 1. Multiple Choice ───────────────────────────────────────────────

    /** GET /api/games/multiple-choice?deckId=&count=10 */
    @GetMapping("/multiple-choice")
    public ApiResponse<List<MultipleChoiceQuestion>> multipleChoice(
            @RequestParam UUID deckId,
            @RequestParam(defaultValue = "10") int count) {
        return ApiResponse.ok(service.generateMultipleChoice(deckId, count, currentUserId()));
    }

    /**
     * POST /api/games/multiple-choice/answer — { "cardId": "...", "selected": "nghĩa user chọn" }
     * Chấm đúng/sai và cập nhật lịch ôn FSRS.
     */
    @PostMapping("/multiple-choice/answer")
    public ApiResponse<AnswerResult> answer(@RequestBody AnswerRequest req) {
        return ApiResponse.ok(service.checkAnswer(req.cardId(), req.selected(), currentUserId()));
    }

    // ── 2. Matching ──────────────────────────────────────────────────────

    /** GET /api/games/matching?deckId=&count=6 */
    @GetMapping("/matching")
    public ApiResponse<MatchingGame> matching(
            @RequestParam UUID deckId,
            @RequestParam(defaultValue = "6") int count) {
        return ApiResponse.ok(service.generateMatching(deckId, count, currentUserId()));
    }

    /** POST /api/games/matching/check — { "pairs": [{"cardId":"...", "matchedBack":"..."}] } */
    @PostMapping("/matching/check")
    public ApiResponse<MatchingCheckResult> checkMatching(@RequestBody MatchingCheckRequest req) {
        return ApiResponse.ok(service.checkMatching(req.pairs(), currentUserId()));
    }

    // ── 3. Typing Recall ─────────────────────────────────────────────────

    /** GET /api/games/typing?deckId=&count=10 */
    @GetMapping("/typing")
    public ApiResponse<List<TypingQuestion>> typing(
            @RequestParam UUID deckId,
            @RequestParam(defaultValue = "10") int count) {
        return ApiResponse.ok(service.generateTyping(deckId, count, currentUserId()));
    }

    /**
     * POST /api/games/typing/check — { "cardId": "...", "typed": "user input" }
     * Levenshtein ≤ 1 → đúng. Cập nhật lịch ôn FSRS.
     */
    @PostMapping("/typing/check")
    public ApiResponse<TypingCheckResult> checkTyping(@RequestBody TypingCheckRequest req) {
        return ApiResponse.ok(service.checkTyping(req.cardId(), req.typed(), currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
