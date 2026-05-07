package com.englishapp.session;

import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.CardService;
import com.englishapp.flashcard.CardSource;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.flashcard.dto.CreateCardRequest;
import com.englishapp.session.dto.AddVocabRequest;
import com.englishapp.user.UserService;
import com.englishapp.video.dto.SubtitleSegmentResponse;
import com.englishapp.video.subtitle.SubtitleService;
import com.englishapp.vocab.VocabMapper;
import com.englishapp.vocab.VocabRepository;
import com.englishapp.vocab.dto.VocabResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/listen")
@RequiredArgsConstructor
public class ListenController {

    private final SessionRepository sessionRepository;
    private final SubtitleService subtitleService;
    private final VocabRepository vocabRepository;
    private final VocabMapper vocabMapper;
    private final CardService cardService;
    private final UserService userService;

    @GetMapping("/subtitles")
    public ApiResponse<List<SubtitleSegmentResponse>> getSubtitles(@PathVariable UUID sessionId) {
        LearningSession session = requireSession(sessionId, currentUserId());
        return ApiResponse.ok(subtitleService.getSegmentResponses(session.getVideoId()));
    }

    @PostMapping("/add-vocab")
    public ApiResponse<CardResponse> addVocab(@PathVariable UUID sessionId,
                                              @Valid @RequestBody AddVocabRequest request) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);

        String contextSentence = null;
        if (request.getSegmentId() != null) {
            contextSentence = subtitleService.getSegments(session.getVideoId()).stream()
                    .filter(s -> s.getId().equals(request.getSegmentId()))
                    .map(s -> s.getText())
                    .findFirst()
                    .orElse(null);
        }

        CreateCardRequest cardRequest = new CreateCardRequest();
        cardRequest.setVocabId(request.getVocabId());
        cardRequest.setSourceVideoId(session.getVideoId());
        cardRequest.setSourceSegmentId(request.getSegmentId());
        cardRequest.setContextSentence(contextSentence);
        cardRequest.setSourceType(CardSource.LISTEN);
        return ApiResponse.ok(cardService.addCard(cardRequest, userId));
    }

    @GetMapping("/vocab-info")
    public ApiResponse<VocabResponse> getVocabInfo(@PathVariable UUID sessionId,
                                                   @RequestParam String word) {
        requireSession(sessionId, currentUserId());
        return ApiResponse.ok(
                vocabRepository.findFirstByWordIgnoreCase(word)
                        .map(vocabMapper::toVocabResponse)
                        .orElse(null)
        );
    }

    private LearningSession requireSession(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return session;
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
