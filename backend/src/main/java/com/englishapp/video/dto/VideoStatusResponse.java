package com.englishapp.video.dto;

import com.englishapp.video.VideoStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VideoStatusResponse {
    private UUID id;
    private VideoStatus status;
    private String errorMessage;
}
