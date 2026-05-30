package com.englishapp.sm2;

import com.englishapp.common.ApiResponse;
import com.englishapp.sm2.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sm2")
@RequiredArgsConstructor
public class Sm2Controller {

    private final Sm2Service    service;
    private final ImportService importService;
    private final SoundsToPracticeService soundsToPractice;

    // ── Decks ───────────────────────────────────────────────────────────────

    @PostMapping("/decks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Sm2Deck> createDeck(@RequestBody DeckRequest req) {
        return ApiResponse.ok(service.createDeck(req));
    }

    /**
     * GET /api/sm2/decks/sounds-to-practice
     * Deck hệ thống chứa các từ/âm phát âm yếu (tự tạo nếu chưa có).
     * Trả deck + cards như deck thường.
     */
    @GetMapping("/decks/sounds-to-practice")
    public ApiResponse<SoundsDeckResponse> soundsToPracticeDeck() {
        Sm2Deck deck = soundsToPractice.getOrCreateDeck();
        return ApiResponse.ok(new SoundsDeckResponse(deck, soundsToPractice.listCards()));
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

    // ── Import ───────────────────────────────────────────────────────────────

    /**
     * POST /api/sm2/decks/{id}/import/text
     * Body: { "content": "...", "termSep": "\t", "cardSep": "\n" }
     * termSep/cardSep hỗ trợ escape: "\\t" = TAB, "\\n" = newline.
     */
    @PostMapping("/decks/{id}/import/text")
    public ApiResponse<ImportSummary> importText(
            @PathVariable UUID id,
            @RequestBody ImportTextRequest req) {
        return ApiResponse.ok(importService.importText(
                id, req.content(), req.termSep(), req.cardSep()));
    }

    /**
     * POST /api/sm2/decks/{id}/import/csv  (multipart)
     * Header tự động bỏ qua nếu dòng đầu là "front,back[,ipa[,example]]".
     */
    @PostMapping(value = "/decks/{id}/import/csv",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSummary> importCsv(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(importService.importCsv(id, file.getInputStream()));
    }

    /**
     * POST /api/sm2/decks/{id}/import/json  (multipart)
     * Body: JSON array — [{front, back, ipa?, exampleSentence?}].
     */
    @PostMapping(value = "/decks/{id}/import/json",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSummary> importJson(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(importService.importJson(id, file.getInputStream()));
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
