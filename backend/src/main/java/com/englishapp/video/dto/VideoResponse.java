package com.englishapp.video.dto;

import com.englishapp.user.CEFRLevel;
import com.englishapp.video.VideoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VideoResponse {
    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationSec;
    private CEFRLevel cefrLevel;
    private String topic;
    private VideoStatus status;
    private int viewCount;
    private Instant createdAt;
}
