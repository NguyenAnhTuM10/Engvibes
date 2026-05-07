package com.englishapp.video.dto;

import com.englishapp.user.CEFRLevel;
import com.englishapp.video.VideoStatus;
import lombok.Data;

@Data
public class VideoFilter {
    private CEFRLevel cefrLevel;
    private String topic;
    private String search;
    private VideoStatus status;
}
