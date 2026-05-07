package com.englishapp.pipeline;

import com.englishapp.storage.StorageService;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingPipeline {

    private final VideoService videoService;
    private final StorageService storageService;
    private final SubtitleService subtitleService;

    @Value("${app.storage.bucket-audios}")
    private String audiosBucket;

    @Async("videoProcessingExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<Void> processVideo(UUID videoId) {
        log.info("[{}] Pipeline start for video {}", Thread.currentThread().getName(), videoId);
        videoService.setVideoStatus(videoId, VideoStatus.PROCESSING, null);

        try {
            String audioKey = "videos/" + videoId + "/audio.mp3";
            byte[] audioBytes = storageService.download(audiosBucket, audioKey);
            subtitleService.processVideoTranscription(videoId, audioBytes);

            videoService.setVideoStatus(videoId, VideoStatus.PUBLISHED, null);
            log.info("[{}] Pipeline done — video {} is PUBLISHED", Thread.currentThread().getName(), videoId);
        } catch (Exception e) {
            log.error("[{}] Pipeline failed for video {}: {}", Thread.currentThread().getName(), videoId, e.getMessage());
            videoService.setVideoStatus(videoId, VideoStatus.FAILED, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }
}
