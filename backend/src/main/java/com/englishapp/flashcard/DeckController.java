package com.englishapp.flashcard;

import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.dto.*;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Flashcard Decks", description = "Manage flashcard decks, list cards and bulk import")
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;
    private final CardService cardService;
    private final CardImportService importService;
    private final UserService userService;

    @GetMapping
    public ApiResponse<List<DeckResponse>> listDecks() {
        UUID userId = currentUserId();
        return ApiResponse.ok(deckService.getUserDecks(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DeckResponse> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        return ApiResponse.ok(deckService.createDeck(request, currentUserId()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<DeckResponse> updateDeck(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateDeckRequest request) {
        return ApiResponse.ok(deckService.updateDeck(id, request, currentUserId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable UUID id) {
        deckService.deleteDeck(id, currentUserId());
    }

    @GetMapping("/{id}/cards")
    public ApiResponse<Page<CardResponse>> getCards(@PathVariable UUID id,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(cardService.getCardsInDeck(id, pageable, currentUserId()));
    }

    @GetMapping("/{id}/cards/due")
    public ApiResponse<List<CardResponse>> getDueCards(@PathVariable UUID id,
                                                       @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(cardService.getDueCards(id, limit, currentUserId()));
    }

    // ── Bulk import (card tự tạo) ───────────────────────────────────────────

    @Operation(summary = "Import from pasted text",
               description = "Body: { content, termSep, cardSep }. Separators accept escapes: \"\\\\t\" = TAB, \"\\\\n\" = newline. "
                           + "Each line: front<termSep>back[<termSep>ipa[<termSep>example]]")
    @PostMapping("/{id}/import/text")
    public ApiResponse<ImportSummary> importText(@PathVariable UUID id,
                                                 @RequestBody ImportTextRequest request) {
        return ApiResponse.ok(importService.importText(
                id, currentUserId(), request.content(), request.termSep(), request.cardSep()));
    }

    @Operation(summary = "Import from CSV file",
               description = "Columns: front,back[,ipa[,exampleSentence]]. Header row optional.")
    @PostMapping(value = "/{id}/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSummary> importCsv(@PathVariable UUID id,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(importService.importCsv(id, currentUserId(), file.getInputStream()));
    }

    @Operation(summary = "Import from JSON file",
               description = "JSON array: [{front, back, ipa?, exampleSentence?}]")
    @PostMapping(value = "/{id}/import/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSummary> importJson(@PathVariable UUID id,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(importService.importJson(id, currentUserId(), file.getInputStream()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
