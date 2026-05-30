package com.englishapp.pronunciation;

import com.englishapp.common.ApiResponse;
import com.englishapp.pronunciation.dto.AttemptResponse;
import com.englishapp.pronunciation.dto.CreateSessionRequest;
import com.englishapp.pronunciation.dto.PronunciationSentence;
import com.englishapp.pronunciation.dto.PronunciationWord;
import com.englishapp.pronunciation.dto.SessionResponse;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pronunciation")
@RequiredArgsConstructor
@Tag(name = "Pronunciation", description = "English pronunciation assessment")
public class PronunciationController {

    private final PronunciationService service;
    private final PronunciationPipeline pipeline;
    private final PronunciationContentService contentService;
    private final UserService userService;

    @Operation(summary = "List practice words — optionally filtered by phoneme group")
    @GetMapping("/words")
    public ApiResponse<List<PronunciationWord>> getWords(@RequestParam(required = false) String group) {
        return ApiResponse.ok(contentService.getWords(group));
    }

    @Operation(summary = "List practice sentences — optionally filtered by category")
    @GetMapping("/sentences")
    public ApiResponse<List<PronunciationSentence>> getSentences(@RequestParam(required = false) String category) {
        return ApiResponse.ok(contentService.getSentences(category));
    }

    @Operation(summary = "Create pronunciation session for a word or sentence")
    @PostMapping("/sessions")
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest req) {
        UUID userId = currentUserId();
        return ApiResponse.ok(service.createSession(userId, req));
    }

    @Operation(summary = "Get session info (includes targetIpa and best score)")
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<SessionResponse> getSession(@PathVariable UUID sessionId) {
        return ApiResponse.ok(service.getSession(sessionId, currentUserId()));
    }

    @Operation(summary = "List all completed attempts for a session")
    @GetMapping("/sessions/{sessionId}/attempts")
    public ApiResponse<List<AttemptResponse>> getAttempts(@PathVariable UUID sessionId) {
        return ApiResponse.ok(service.getAttempts(sessionId, currentUserId()));
    }

    /**
     * Submit audio recording.
     * Trả về 202 + attemptId ngay lập tức.
     * Kết quả phân tích đến qua WebSocket: /topic/pronunciation/{sessionId}
     *
     * WS message types: PROCESSING → TRANSCRIBED → COMPLETED (hoặc FAILED)
     */
    @Operation(summary = "Submit audio recording — returns attemptId immediately, result via WebSocket")
    @PostMapping("/sessions/{sessionId}/attempt")
    public ResponseEntity<ApiResponse<UUID>> submitAttempt(
            @PathVariable UUID sessionId,
            @RequestParam("audio") MultipartFile audio) {

        UUID userId = currentUserId();

        // Đọc bytes trong request thread vì MultipartFile là request-scoped
        byte[] audioBytes;
        String filename, contentType;
        try {
            audioBytes  = audio.getBytes();
            filename    = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm";
            contentType = audio.getContentType() != null ? audio.getContentType() : "audio/webm";
        } catch (Exception e) {
            throw com.englishapp.common.ApiException.badRequest("Failed to read audio: " + e.getMessage());
        }

        // Tạo placeholder attempt row — lấy ID để trả về cho client
        UUID attemptId = service.createAttemptPlaceholder(sessionId, userId);
        log.info("[Pronunciation] session={} attempt={} queued", sessionId, attemptId);

        // Kick off async pipeline (tách biệt Spring bean → AOP proxy hoạt động)
        pipeline.processAsync(sessionId, userId, attemptId, audioBytes, filename, contentType);

        return ResponseEntity.accepted().body(ApiResponse.ok(attemptId));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
