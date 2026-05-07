package com.englishapp.video.dto;

import com.englishapp.ai.dto.WarmupWord;
import com.englishapp.user.CEFRLevel;
import com.englishapp.video.VideoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    // Enrichment fields (populated after pipeline completes)
    private String summary;
    private List<String> keyPoints;
    private String speakingQuestion;
    private List<WarmupWord> warmupWords;
    private Map<String, List<String>> collocations;
}
