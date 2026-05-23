package com.englishapp.retell.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RetellScaffoldResponse {
    private int level;
    private List<String> wordBank;
    private List<String> sentenceStarters;
    private String storyFrame;
}
