package com.englishapp.pipeline;

import com.englishapp.ai.AIOrchestrationService;
import com.englishapp.ai.dto.VideoEnrichment;
import com.englishapp.ai.dto.VideoSummary;
import com.englishapp.notification.NotificationMessage;
import com.englishapp.notification.NotificationService;
import com.englishapp.notification.NotificationType;
import com.englishapp.storage.StorageService;
import com.englishapp.video.Video;
import com.englishapp.video.VideoService;
import com.englishapp.video.VideoStatus;
import com.englishapp.video.subtitle.SubtitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingPipeline {

    private final VideoService videoService;
    private final StorageService storageService;
    private final SubtitleService subtitleService;
    private final AIOrchestrationService aiOrchestrationService;
    private final NotificationService notificationService;

    @Value("${app.storage.bucket-audios}")
    private String audiosBucket;

    @Async("videoProcessingExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<Void> processVideo(UUID videoId) {
        log.info("[{}] Pipeline start for video {}", Thread.currentThread().getName(), videoId);
        videoService.setVideoStatus(videoId, VideoStatus.PROCESSING, null);

        try {
            // Step 1: Whisper transcription (critical — failure stops pipeline)
            String audioKey = "videos/" + videoId + "/audio.mp3";
            byte[] audioBytes = storageService.download(audiosBucket, audioKey);
            subtitleService.processVideoTranscription(videoId, audioBytes);

            // Step 2: NLP + LLM enrichment (non-critical — failure still publishes)
            try {
                Video video = videoService.getVideoById(videoId);
                VideoEnrichment enrichment = aiOrchestrationService.enrichVideo(videoId, video.getCefrLevel());
                VideoSummary summary = aiOrchestrationService.generateVideoSummary(videoId);
                videoService.saveEnrichment(videoId, enrichment, summary);
            } catch (Exception e) {
                log.warn("[{}] Enrichment failed for video {} (non-critical): {}",
                        Thread.currentThread().getName(), videoId, e.getMessage());
            }

            videoService.setVideoStatus(videoId, VideoStatus.PUBLISHED, null);
            log.info("[{}] Pipeline done — video {} is PUBLISHED", Thread.currentThread().getName(), videoId);
            notificationService.sendToAdmins(NotificationMessage.builder()
                    .type(NotificationType.VIDEO_PUBLISHED)
                    .title("Video published")
                    .message("Video " + videoId + " is now published")
                    .data(Map.of("videoId", videoId.toString()))
                    .build());
        } catch (Exception e) {
            log.error("[{}] Pipeline failed for video {}: {}", Thread.currentThread().getName(), videoId, e.getMessage());
            videoService.setVideoStatus(videoId, VideoStatus.FAILED, e.getMessage());
            notificationService.sendToAdmins(NotificationMessage.builder()
                    .type(NotificationType.VIDEO_FAILED)
                    .title("Video processing failed")
                    .message("Video " + videoId + " failed: " + e.getMessage())
                    .data(Map.of("videoId", videoId.toString()))
                    .build());
        }

        return CompletableFuture.completedFuture(null);
    }
}
