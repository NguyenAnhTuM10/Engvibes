package com.englishapp.video.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SubtitleSegmentResponse {
    private UUID id;
    private int orderIndex;
    private int startMs;
    private int endMs;
    private String text;
    private String wordTimings;
}
