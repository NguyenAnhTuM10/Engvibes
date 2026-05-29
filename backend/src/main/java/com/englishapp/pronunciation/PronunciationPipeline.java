package com.englishapp.pronunciation;

import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.pronunciation.dto.AnalyzeResult;
import com.englishapp.pronunciation.dto.AttemptResponse;
import com.englishapp.pronunciation.dto.PronunciationProgress;
import com.englishapp.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PronunciationPipeline {

    private final PronunciationService pronunciationService;
    private final WhisperClient whisperClient;
    private final PronunciationAiClient aiClient;
    private final StorageService storageService;
    private final SimpMessagingTemplate ws;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    /**
     * Async pipeline: Whisper → Python analysis → save → WS push.
     *
     * Dùng NOT_SUPPORTED để tránh rollback-only khi external service ném exception
     * (xem BUG pattern trong CLAUDE.md).
     */
    @Async("pronunciationExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processAsync(UUID sessionId, UUID userId, UUID attemptId,
                             byte[] audioBytes, String filename, String contentType) {

        // WS topic riêng cho từng session — client subscribe trước khi POST
        String topic = "/topic/pronunciation/" + sessionId;

        try {
            // ── Bước 1: upload audio lên MinIO ─────────────────────────
            push(topic, PronunciationProgress.processing(attemptId, 10, "Uploading audio..."));
            String audioKey = String.format("pronunciation/%s/%s.webm", sessionId, attemptId);
            storageService.upload(recordingsBucket, audioKey,
                    new ByteArrayInputStream(audioBytes), audioBytes.length, contentType);

            // ── Bước 2: Whisper transcription ───────────────────────────
            push(topic, PronunciationProgress.processing(attemptId, 25, "Transcribing speech..."));
            WhisperResult whisperResult = whisperClient.transcribe(audioBytes, filename, contentType);
            String transcript = whisperResult != null && whisperResult.getText() != null
                    ? whisperResult.getText().trim() : "";

            push(topic, PronunciationProgress.transcribed(attemptId, transcript));
            log.info("[Pronunciation] attempt={} transcript='{}'", attemptId, transcript);

            // ── Bước 3: IPA comparison + scoring (Python service) ───────
            push(topic, PronunciationProgress.processing(attemptId, 65, "Analyzing phonemes..."));
            PronunciationSession session = pronunciationService.requireOwned(sessionId, userId);
            AnalyzeResult result = aiClient.analyze(transcript, session.getTargetText(), session.getTargetIpa());

            // ── Bước 4: lưu kết quả vào DB ──────────────────────────────
            push(topic, PronunciationProgress.processing(attemptId, 85, "Saving results..."));
            AttemptResponse response = pronunciationService.saveAttemptResult(
                    attemptId, audioKey, transcript, result);

            // ── Bước 5: push kết quả cuối về frontend ───────────────────
            push(topic, PronunciationProgress.completed(attemptId, response));
            log.info("[Pronunciation] attempt={} score={}", attemptId, result.overallScore());

        } catch (Exception e) {
            log.error("[Pronunciation] attempt={} FAILED: {}", attemptId, e.getMessage(), e);
            push(topic, PronunciationProgress.failed(attemptId, "Processing failed: " + e.getMessage()));
        }
    }

    private void push(String topic, PronunciationProgress payload) {
        ws.convertAndSend(topic, payload);
    }
}
