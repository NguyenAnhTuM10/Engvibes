package com.englishapp.session;

import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.flashcard.CardService;
import com.englishapp.flashcard.FlashcardMapper;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.flashcard.dto.ReviewCardRequest;
import com.englishapp.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/quick-review")
@RequiredArgsConstructor
public class QuickReviewController {

    private final SessionRepository sessionRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;
    private final FlashcardMapper flashcardMapper;
    private final UserService userService;

    @GetMapping
    public ApiResponse<List<CardResponse>> listCards(@PathVariable UUID sessionId) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);
        List<CardResponse> cards = cardRepository
                .findByUserIdAndSourceVideoId(userId, session.getVideoId())
                .stream()
                .map(flashcardMapper::toCardResponse)
                .toList();
        return ApiResponse.ok(cards);
    }

    @PostMapping("/review/{cardId}")
    public ApiResponse<CardResponse> reviewCard(@PathVariable UUID sessionId,
                                                @PathVariable UUID cardId,
                                                @Valid @RequestBody ReviewCardRequest request) {
        requireSession(sessionId, currentUserId());
        return ApiResponse.ok(cardService.reviewCard(cardId, request.getRating(), currentUserId()));
    }

    private LearningSession requireSession(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return session;
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
