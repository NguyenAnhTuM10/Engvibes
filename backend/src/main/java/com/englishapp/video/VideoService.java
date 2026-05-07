package com.englishapp.video;

import com.englishapp.common.ApiException;
import com.englishapp.storage.StorageService;
import com.englishapp.video.dto.CreateVideoRequest;
import com.englishapp.video.dto.UpdateVideoRequest;
import com.englishapp.video.dto.VideoFilter;
import com.englishapp.video.dto.VideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Propagation;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final FfmpegService ffmpegService;
    private final com.englishapp.video.subtitle.SubtitleService subtitleService;

    @Value("${app.storage.bucket-videos}")
    private String videosBucket;

    @Value("${app.storage.bucket-audios}")
    private String audiosBucket;

    private static final Duration URL_EXPIRY = Duration.ofHours(1);

    public VideoResponse uploadVideo(MultipartFile file, CreateVideoRequest request) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw ApiException.badRequest("File must be a video (mp4, webm, etc.)");
        }

        UUID videoId = UUID.randomUUID();
        String sourceKey = "videos/" + videoId + "/source.mp4";

        // Save to temp file first (stream can only be read once)
        File tempVideo = null;
        File tempAudio = null;
        File tempThumb = null;
        try {
            tempVideo = Files.createTempFile("video-upload-", ".mp4").toFile();
            file.transferTo(tempVideo);

            // Upload source to MinIO
            storageService.upload(videosBucket, sourceKey,
                    Files.newInputStream(tempVideo.toPath()), tempVideo.length(), "video/mp4");

            // Build initial Video record
            Video video = Video.builder()
                    .id(videoId)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .topic(request.getTopic())
                    .cefrLevel(request.getCefrLevel())
                    .storageUrl(sourceKey)
                    .status(VideoStatus.DRAFT)
                    .build();

            // FFmpeg processing
            try {
                int durationSec = ffmpegService.getDurationSec(tempVideo);
                video.setDurationSec(durationSec);

                tempAudio = ffmpegService.extractAudio(tempVideo);
                String audioKey = "videos/" + videoId + "/audio.mp3";
                storageService.upload(audiosBucket, audioKey,
                        Files.newInputStream(tempAudio.toPath()), tempAudio.length(), "audio/mpeg");

                double thumbAt = durationSec > 2 ? durationSec / 2.0 : 0;
                tempThumb = ffmpegService.extractThumbnail(tempVideo, thumbAt);
                String thumbKey = "videos/" + videoId + "/thumbnail.jpg";
                storageService.upload(videosBucket, thumbKey,
                        Files.newInputStream(tempThumb.toPath()), tempThumb.length(), "image/jpeg");
                video.setThumbnailUrl(thumbKey);

                log.info("Video processed: {} — {}s, audio+thumbnail extracted", video.getTitle(), durationSec);
            } catch (Exception e) {
                log.warn("FFmpeg processing failed for video {}: {}", videoId, e.getMessage());
                video.setErrorMessage(e.getMessage());
                video.setStatus(VideoStatus.FAILED);
            }

            video = videoRepository.save(video);
            return toResponse(video);

        } catch (IOException e) {
            log.error("Failed to process video upload", e);
            throw ApiException.badRequest("Failed to process uploaded file");
        } finally {
            deleteSilently(tempVideo);
            deleteSilently(tempAudio);
            deleteSilently(tempThumb);
        }
    }

    @Transactional(readOnly = true)
    public VideoResponse getVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        return toResponse(video);
    }

    @Transactional
    public VideoResponse getPublishedVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw ApiException.notFound("Video not found");
        }
        videoRepository.incrementViewCount(id);
        return toResponse(video);
    }

    @Transactional(readOnly = true)
    public Page<VideoResponse> listVideos(VideoFilter filter, Pageable pageable) {
        VideoStatus status = filter.getStatus() != null ? filter.getStatus() : VideoStatus.PUBLISHED;
        Page<Video> page;

        String search = filter.getSearch();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch) {
            page = videoRepository.searchByStatusAndQuery(status, search, pageable);
        } else if (filter.getCefrLevel() != null && filter.getTopic() != null) {
            page = videoRepository.findByStatusAndCefrLevelAndTopic(status, filter.getCefrLevel(), filter.getTopic(), pageable);
        } else if (filter.getCefrLevel() != null) {
            page = videoRepository.findByStatusAndCefrLevel(status, filter.getCefrLevel(), pageable);
        } else if (filter.getTopic() != null) {
            page = videoRepository.findByStatusAndTopic(status, filter.getTopic(), pageable);
        } else {
            page = videoRepository.findByStatus(status, pageable);
        }

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VideoResponse> listAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable).map(this::toResponse);
    }

    public VideoResponse updateVideo(UUID id, UpdateVideoRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        if (request.getTitle() != null) video.setTitle(request.getTitle());
        if (request.getDescription() != null) video.setDescription(request.getDescription());
        if (request.getTopic() != null) video.setTopic(request.getTopic());
        if (request.getCefrLevel() != null) video.setCefrLevel(request.getCefrLevel());
        video.setUpdatedAt(Instant.now());
        return toResponse(videoRepository.save(video));
    }

    public void deleteVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        video.setDeletedAt(Instant.now());
        videoRepository.save(video);
    }

    // NOT_SUPPORTED: chạy ngoài transaction để catch block có thể save FAILED
    // nếu không, exception trong subtitleService mark transaction rollback-only
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public VideoResponse processVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Video not found"));

        if (video.getStatus() != VideoStatus.DRAFT && video.getStatus() != VideoStatus.FAILED) {
            throw ApiException.badRequest("Video must be in DRAFT or FAILED status to process");
        }

        setVideoStatus(id, VideoStatus.PROCESSING, null);

        try {
            String audioKey = "videos/" + id + "/audio.mp3";
            byte[] audioBytes = storageService.download(audiosBucket, audioKey);
            subtitleService.processVideoTranscription(id, audioBytes);
            log.info("Transcription done for video {}", id);
        } catch (Exception e) {
            log.error("Video processing failed for {}: {}", id, e.getMessage());
            setVideoStatus(id, VideoStatus.FAILED, e.getMessage());
        }

        return toResponse(videoRepository.findById(id).orElseThrow());
    }

    @Transactional
    public void setVideoStatus(UUID id, VideoStatus status, String errorMessage) {
        Video v = videoRepository.findById(id).orElseThrow();
        v.setStatus(status);
        v.setErrorMessage(errorMessage);
        v.setUpdatedAt(Instant.now());
        videoRepository.save(v);
    }

    private void deleteSilently(File file) {
        if (file != null && file.exists()) {
            try { Files.delete(file.toPath()); } catch (IOException ignored) {}
        }
    }

    private VideoResponse toResponse(Video video) {
        String videoUrl = null;
        if (video.getStorageUrl() != null) {
            videoUrl = storageService.generatePresignedUrl(videosBucket, video.getStorageUrl(), URL_EXPIRY);
        }

        String thumbnailUrl = null;
        if (video.getThumbnailUrl() != null) {
            thumbnailUrl = storageService.generatePresignedUrl(videosBucket, video.getThumbnailUrl(), URL_EXPIRY);
        }

        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .videoUrl(videoUrl)
                .durationSec(video.getDurationSec())
                .cefrLevel(video.getCefrLevel())
                .topic(video.getTopic())
                .status(video.getStatus())
                .viewCount(video.getViewCount())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
