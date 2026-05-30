package com.englishapp.pronunciation;

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
    private final PronunciationAiClient aiClient;
    private final StorageService storageService;
    private final SimpMessagingTemplate ws;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    /**
     * Async pipeline: upload → wav2vec2 phoneme analysis → save → WS push.
     * Bỏ Whisper: Python service nhận audio trực tiếp, dùng wav2vec2 để tránh
     * ASR auto-correct che giấu lỗi phát âm.
     * Dùng NOT_SUPPORTED tránh rollback-only (xem BUG pattern trong CLAUDE.md).
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

            // ── Bước 2: wav2vec2 phoneme analysis (Python service) ──────
            // Bỏ Whisper — gửi audio trực tiếp, Python dùng wav2vec2 phân tích phoneme.
            // Tránh auto-correct của Whisper (vd "tink"→"think") che giấu lỗi phát âm.
            push(topic, PronunciationProgress.processing(attemptId, 30, "Analyzing phonemes..."));
            PronunciationSession session = pronunciationService.requireOwned(sessionId, userId);
            AnalyzeResult result = aiClient.analyze(audioBytes, contentType,
                    session.getTargetText(), session.getTargetIpa());

            // actual_ipa là "transcript" hiển thị cho user (phoneme detected)
            String transcript = result.actualIpa() != null ? result.actualIpa() : "";
            push(topic, PronunciationProgress.transcribed(attemptId, transcript));
            log.info("[Pronunciation] attempt={} actual_ipa='{}'", attemptId, transcript);

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
