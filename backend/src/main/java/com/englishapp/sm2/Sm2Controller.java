package com.englishapp.sm2;

import com.englishapp.common.ApiResponse;
import com.englishapp.sm2.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sm2")
@RequiredArgsConstructor
public class Sm2Controller {

    private final Sm2Service service;

    // ── Decks ───────────────────────────────────────────────────────────────

    @PostMapping("/decks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Sm2Deck> createDeck(@RequestBody DeckRequest req) {
        return ApiResponse.ok(service.createDeck(req));
    }

    @GetMapping("/decks")
    public ApiResponse<List<Sm2Deck>> listDecks() {
        return ApiResponse.ok(service.listDecks());
    }

    @GetMapping("/decks/{id}/cards")
    public ApiResponse<List<Sm2Card>> listCards(@PathVariable UUID id) {
        return ApiResponse.ok(service.listCards(id));
    }

    // ── Cards ────────────────────────────────────────────────────────────────

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Sm2Card> createCard(@RequestBody CardRequest req) {
        return ApiResponse.ok(service.createCard(req));
    }

    // ── Review queue ─────────────────────────────────────────────────────────

    /**
     * GET /api/sm2/review/queue?deck_id=&limit=20
     * Trả card đến hạn (due_date <= now) + card mới (chưa có review).
     * Sắp theo due_date tăng dần — quá hạn lâu nhất lên trước.
     */
    @GetMapping("/review/queue")
    public ApiResponse<List<QueueItem>> queue(
            @RequestParam(required = false) UUID deck_id,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.getQueue(deck_id, Math.min(limit, 100)));
    }

    // ── Review ───────────────────────────────────────────────────────────────

    /**
     * POST /api/sm2/review/{cardId}
     * Body: { "quality": 0-5 }
     * Chạy SM-2, trả trạng thái SRS mới.
     */
    @PostMapping("/review/{cardId}")
    public ApiResponse<ReviewResult> review(
            @PathVariable UUID cardId,
            @RequestBody ReviewRequest req) {
        return ApiResponse.ok(service.review(cardId, req.quality()));
    }
}
