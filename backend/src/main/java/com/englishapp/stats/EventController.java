package com.englishapp.stats;

import com.englishapp.common.ApiResponse;
import com.englishapp.stats.dto.BatchEventRequest;
import com.englishapp.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Event Tracking", description = "Batch track user behavior events")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventTrackingService eventTrackingService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> trackBatch(@Valid @RequestBody BatchEventRequest request) {
        UUID userId = userService.getCurrentUser().getId();
        eventTrackingService.trackBatch(userId, request.getEvents());
        return ApiResponse.ok(null);
    }
}
