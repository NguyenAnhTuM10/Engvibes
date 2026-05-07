package com.englishapp.session;

import com.englishapp.common.ApiResponse;
import com.englishapp.session.dto.*;
import com.englishapp.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> createOrGetSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(sessionService.getOrCreateSession(currentUserId(), request.getVideoId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionResponse> getSession(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.getSession(id, currentUserId()));
    }

    @PatchMapping("/{id}/step")
    public ApiResponse<SessionResponse> advanceStep(@PathVariable UUID id,
                                                    @Valid @RequestBody AdvanceStepRequest request) {
        return ApiResponse.ok(sessionService.advanceStep(id, currentUserId(), request));
    }

    @PatchMapping("/{id}/scaffold")
    public ApiResponse<SessionResponse> setScaffold(@PathVariable UUID id,
                                                    @Valid @RequestBody SetScaffoldRequest request) {
        return ApiResponse.ok(sessionService.setScaffoldLevel(id, currentUserId(), request));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<SessionResponse> finish(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.finishSession(id, currentUserId()));
    }

    @GetMapping("/history")
    public ApiResponse<Page<SessionResponse>> history(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(sessionService.getHistory(currentUserId(), pageable));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
