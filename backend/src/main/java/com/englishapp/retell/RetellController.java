package com.englishapp.retell;

import com.englishapp.common.ApiResponse;
import com.englishapp.retell.dto.*;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Step 5 - Retell", description = "AI-coached retell with scaffold L1-L4")
@RestController
@RequestMapping("/api/sessions/{sessionId}/retell")
@RequiredArgsConstructor
public class RetellController {

    private final RetellService retellService;
    private final UserService userService;

    @PostMapping("/start")
    public ApiResponse<RetellScaffoldResponse> startRetell(@PathVariable UUID sessionId,
                                                           @Valid @RequestBody StartRetellRequest request) {
        return ApiResponse.ok(retellService.startRetell(sessionId, currentUserId(), request));
    }

    @PostMapping("/attempt")
    public ApiResponse<RetellFeedbackResponse> submitAttempt(@PathVariable UUID sessionId,
                                                             @RequestParam MultipartFile audio) {
        return ApiResponse.ok(retellService.submitAttempt(sessionId, currentUserId(), audio));
    }

    @GetMapping("/attempts")
    public ApiResponse<List<RetellAttemptSummary>> getAttempts(@PathVariable UUID sessionId) {
        return ApiResponse.ok(retellService.getAttempts(sessionId, currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
