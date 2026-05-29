package com.englishapp.pronunciation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Payload đẩy qua WebSocket /topic/pronunciation/{sessionId}
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationProgress {

    private UUID attemptId;
    private String type;       // PROCESSING | TRANSCRIBED | COMPLETED | FAILED
    private int progress;      // 0–100
    private String message;
    private AttemptResponse result;  // chỉ có khi type=COMPLETED

    public static PronunciationProgress processing(UUID id, int pct, String msg) {
        return PronunciationProgress.builder()
                .attemptId(id).type("PROCESSING").progress(pct).message(msg).build();
    }

    public static PronunciationProgress transcribed(UUID id, String transcript) {
        return PronunciationProgress.builder()
                .attemptId(id).type("TRANSCRIBED").progress(50)
                .message(transcript.isBlank() ? "(no speech detected)" : transcript)
                .build();
    }

    public static PronunciationProgress completed(UUID id, AttemptResponse result) {
        return PronunciationProgress.builder()
                .attemptId(id).type("COMPLETED").progress(100).message("Done").result(result)
                .build();
    }

    public static PronunciationProgress failed(UUID id, String error) {
        return PronunciationProgress.builder()
                .attemptId(id).type("FAILED").progress(0).message(error).build();
    }
}
