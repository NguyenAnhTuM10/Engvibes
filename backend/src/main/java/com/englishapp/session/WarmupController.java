package com.englishapp.session;

import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.common.ApiException;
import com.englishapp.common.ApiResponse;
import com.englishapp.flashcard.CardService;
import com.englishapp.flashcard.CardSource;
import com.englishapp.flashcard.dto.CreateCardRequest;
import com.englishapp.session.dto.MarkWarmupRequest;
import com.englishapp.session.dto.WarmupWordResponse;
import com.englishapp.user.UserService;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import com.englishapp.vocab.VocabEntry;
import com.englishapp.vocab.VocabRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionId}/warmup")
@RequiredArgsConstructor
public class WarmupController {

    private final SessionRepository sessionRepository;
    private final VideoRepository videoRepository;
    private final VocabRepository vocabRepository;
    private final CardService cardService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<List<WarmupWordResponse>> getWarmupWords(@PathVariable UUID sessionId) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);
        Video video = videoRepository.findById(session.getVideoId())
                .orElseThrow(() -> ApiException.notFound("Video not found"));

        List<WarmupWord> warmupWords = parseWarmupWords(video.getWarmupWordsJson());
        List<String> wordNames = warmupWords.stream().map(WarmupWord::word).toList();
        Map<String, VocabEntry> vocabByWord = vocabRepository.findByWordIn(wordNames).stream()
                .collect(Collectors.toMap(v -> v.getWord().toLowerCase(), v -> v, (a, b) -> a));

        List<WarmupWordResponse> result = new ArrayList<>();
        for (int i = 0; i < warmupWords.size(); i++) {
            WarmupWord w = warmupWords.get(i);
            VocabEntry vocab = vocabByWord.get(w.word().toLowerCase());
            result.add(WarmupWordResponse.builder()
                    .vocabId(vocab != null ? vocab.getId() : null)
                    .word(w.word())
                    .ipa(w.ipa())
                    .definition(w.definition())
                    .cefrLevel(w.cefrLevel())
                    .partOfSpeech(w.partOfSpeech())
                    .priorityOrder(i + 1)
                    .build());
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/mark")
    public ApiResponse<Void> markWord(@PathVariable UUID sessionId,
                                      @Valid @RequestBody MarkWarmupRequest request) {
        UUID userId = currentUserId();
        LearningSession session = requireSession(sessionId, userId);
        if ("new".equals(request.getStatus())) {
            CreateCardRequest cardRequest = new CreateCardRequest();
            cardRequest.setVocabId(request.getVocabId());
            cardRequest.setSourceVideoId(session.getVideoId());
            cardRequest.setSourceType(CardSource.WARMUP);
            cardService.addCard(cardRequest, userId);
        }
        return ApiResponse.ok(null);
    }

    private LearningSession requireSession(UUID sessionId, UUID userId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return session;
    }

    private List<WarmupWord> parseWarmupWords(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<WarmupWord>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse warmup words JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
