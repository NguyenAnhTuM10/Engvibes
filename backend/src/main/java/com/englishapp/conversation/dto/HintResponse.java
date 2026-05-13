package com.englishapp.conversation.dto;

import java.util.List;

public record HintResponse(List<String> keywords, String exampleSentence) {
    public static HintResponse empty() {
        return new HintResponse(List.of(), "");
    }
}
