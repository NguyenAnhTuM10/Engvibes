package com.englishapp.conversation;

import com.englishapp.common.ApiResponse;
import com.englishapp.conversation.dto.*;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Conversation Practice", description = "Real-time AI roleplay conversation with keyword hints")
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    @GetMapping("/scenarios")
    public ApiResponse<List<ScenarioResponse>> getScenarios() {
        return ApiResponse.ok(conversationService.getScenarios());
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationSessionResponse> startSession(
            @Valid @RequestBody StartConversationRequest request) {
        return ApiResponse.ok(conversationService.startSession(currentUserId(), request.scenarioId()));
    }

    @PostMapping("/{sessionId}/turn")
    public ApiResponse<ConversationTurnResponse> processTurn(
            @PathVariable UUID sessionId,
            @RequestParam MultipartFile audio) {
        return ApiResponse.ok(conversationService.processTurn(sessionId, currentUserId(), audio));
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponse<ConversationEndResponse> endSession(@PathVariable UUID sessionId) {
        return ApiResponse.ok(conversationService.endSession(sessionId, currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
