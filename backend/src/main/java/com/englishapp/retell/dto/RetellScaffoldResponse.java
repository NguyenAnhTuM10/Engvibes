package com.englishapp.retell.dto;

import com.englishapp.ai.dto.WarmupWord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RetellScaffoldResponse {
    private int level;
    private List<WarmupWord> wordBank;
    private List<String> sentenceStarters;
    private String storyFrame;
}
