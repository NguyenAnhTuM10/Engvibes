package com.englishapp.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoSummary {
    private String summary;
    private List<String> keyPoints;
    private String speakingQuestion;

    public static VideoSummary empty() {
        return VideoSummary.builder()
                .summary("")
                .keyPoints(List.of())
                .speakingQuestion("")
                .build();
    }
}
