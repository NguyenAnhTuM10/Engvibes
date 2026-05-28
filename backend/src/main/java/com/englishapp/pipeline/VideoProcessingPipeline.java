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
        step(videoId, NotificationType.PIPELINE_STARTED, "Pipeline started", "Preparing to process video...");

        try {
            // Step 1: Download audio from MinIO
            step(videoId, NotificationType.PIPELINE_EXTRACTING_AUDIO, "Extracting audio", "Downloading audio track from storage...");
            String audioKey = "videos/" + videoId + "/audio.mp3";
            byte[] audioBytes = storageService.download(audiosBucket, audioKey);

            // Step 2: Whisper STT
            step(videoId, NotificationType.PIPELINE_TRANSCRIBING, "Transcribing audio", "Running Whisper speech-to-text...");
            subtitleService.processVideoTranscription(videoId, audioBytes);

            step(videoId, NotificationType.PIPELINE_SAVING_SUBTITLES, "Saving subtitles", "Subtitle segments saved to database.");

            // Step 3: NLP + LLM enrichment (non-critical)
            try {
                step(videoId, NotificationType.PIPELINE_ENRICHING, "Enriching vocabulary", "Running NLP + generating collocations via OpenAI...");
                Video video = videoService.getVideoById(videoId);
                VideoEnrichment enrichment = aiOrchestrationService.enrichVideo(videoId, video.getCefrLevel());

                step(videoId, NotificationType.PIPELINE_SUMMARIZING, "Generating summary", "Generating video summary and speaking question via OpenAI...");
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
                    .message("Video is now live and ready for learners.")
                    .data(Map.of("videoId", videoId.toString()))
                    .build());

        } catch (Exception e) {
            log.error("[{}] Pipeline failed for video {}: {}", Thread.currentThread().getName(), videoId, e.getMessage());
            videoService.setVideoStatus(videoId, VideoStatus.FAILED, e.getMessage());
            notificationService.sendToAdmins(NotificationMessage.builder()
                    .type(NotificationType.VIDEO_FAILED)
                    .title("Processing failed")
                    .message(e.getMessage())
                    .data(Map.of("videoId", videoId.toString()))
                    .build());
        }

        return CompletableFuture.completedFuture(null);
    }

    private void step(UUID videoId, NotificationType type, String title, String message) {
        log.info("[Pipeline][{}] {} — {}", videoId, title, message);
        notificationService.sendToAdmins(NotificationMessage.builder()
                .type(type)
                .title(title)
                .message(message)
                .data(Map.of("videoId", videoId.toString()))
                .build());
    }
}
