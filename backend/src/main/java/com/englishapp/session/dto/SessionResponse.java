package com.englishapp.session.dto;

import com.englishapp.session.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SessionResponse {
    private UUID id;
    private UUID videoId;
    private int currentStep;
    private List<Integer> completedSteps;
    private SessionStatus status;
    private Integer scaffoldLevel;
    private int totalXpEarned;
    private Instant startedAt;
    private Instant completedAt;
}
