package com.englishapp.conversation.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ConversationEndResponse(
        UUID sessionId,
        int totalTurns,
        int xpEarned,
        SummaryData summary
) {
    public record SummaryData(
            List<GrammarError> grammarErrors,
            List<String> vocabHighlights,
            String encouragement,
            String topTip
    ) {}

    public record GrammarError(String error, String correction) {}
}
