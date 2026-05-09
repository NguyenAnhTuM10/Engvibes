package com.englishapp.flashcard;

import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.flashcard.dto.CreateCardRequest;
import com.englishapp.flashcard.dto.ReviewCardRequest;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Flashcard Cards", description = "Manage cards and FSRS review")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CardResponse> addCard(@Valid @RequestBody CreateCardRequest request) {
        return ApiResponse.ok(cardService.addCard(request, currentUserId()));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<CardResponse> reviewCard(@PathVariable UUID id,
                                                @Valid @RequestBody ReviewCardRequest request) {
        return ApiResponse.ok(cardService.reviewCard(id, request.getRating(), currentUserId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id, currentUserId());
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
