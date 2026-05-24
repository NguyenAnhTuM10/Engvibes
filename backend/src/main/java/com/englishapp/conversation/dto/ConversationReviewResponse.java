package com.englishapp.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConversationReviewResponse {
    private double fluency;
    private double grammar;
    private double vocabulary;
    private double overall;
    private String strengths;
    private String improvements;
    private String encouragement;
    private List<GrammarNote> grammarNotes;

    public record GrammarNote(String error, String correction) {}
}
