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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageService storageService;

    @Value("${app.storage.bucket-videos}")
    private String videosBucket;

    private static final Duration URL_EXPIRY = Duration.ofHours(1);

    public VideoResponse uploadVideo(MultipartFile file, CreateVideoRequest request) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw ApiException.badRequest("File must be a video (mp4, webm, etc.)");
        }

        UUID videoId = UUID.randomUUID();
        String key = "videos/" + videoId + "/source.mp4";

        try {
            storageService.upload(videosBucket, key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            log.error("Failed to upload video file", e);
            throw ApiException.badRequest("Failed to read uploaded file");
        }

        Video video = Video.builder()
                .id(videoId)
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(request.getTopic())
                .cefrLevel(request.getCefrLevel())
                .storageUrl(key)
                .status(VideoStatus.DRAFT)
                .build();

        video = videoRepository.save(video);
        log.info("Video uploaded: {} ({})", video.getTitle(), video.getId());
        return toResponse(video);
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
