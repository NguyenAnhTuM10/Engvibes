package com.englishapp.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhisperResult {

    private String text;
    private List<WhisperSegment> segments;
    private List<WhisperWord> words;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhisperSegment {
        private double start;
        private double end;
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhisperWord {
        private String word;
        private double start;
        private double end;
    }
}
