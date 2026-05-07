package com.englishapp.flashcard;

import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.dto.*;
import com.englishapp.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;
    private final CardService cardService;
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
        return ApiResponse.ok(cardService.getDueCards(id, limit));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
