package com.englishapp.shadow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ShadowResultItem {
    private UUID segmentId;
    private int orderIndex;
    private String segmentText;
    private int totalAttempts;
    private double bestAccuracy;
    private ShadowAttemptResponse bestAttempt;
}
