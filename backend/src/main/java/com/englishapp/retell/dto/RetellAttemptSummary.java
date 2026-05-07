package com.englishapp.retell.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RetellAttemptSummary {
    private UUID id;
    private int attemptNumber;
    private int scaffoldLevel;
    private int overallScore;
    private int durationSec;
    private String transcript;
    private RetellFeedback feedback;
    private Instant createdAt;
}
