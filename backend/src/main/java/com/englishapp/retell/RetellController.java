package com.englishapp.retell;

import com.englishapp.common.ApiResponse;
import com.englishapp.retell.dto.*;
import com.englishapp.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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
    public ApiResponse<RetellAttemptSummary> submitAttempt(@PathVariable UUID sessionId,
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
