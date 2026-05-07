package com.englishapp.video.dto;

import com.englishapp.user.CEFRLevel;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVideoRequest {

    @Size(max = 200)
    private String title;

    private String description;

    @Size(max = 50)
    private String topic;

    private CEFRLevel cefrLevel;
}
