package com.englishapp.video;

import com.englishapp.common.ApiResponse;
import com.englishapp.pipeline.VideoProcessingPipeline;
import com.englishapp.video.dto.CreateVideoRequest;
import com.englishapp.video.dto.SubtitleSegmentResponse;
import com.englishapp.video.dto.UpdateVideoRequest;
import com.englishapp.video.dto.VideoFilter;
import com.englishapp.video.dto.VideoResponse;
import com.englishapp.video.dto.VideoStatusResponse;
import com.englishapp.video.subtitle.SubtitleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Videos", description = "Video management (ADMIN only)")
@RestController
@RequestMapping("/api/admin/videos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoService videoService;
    private final VideoProcessingPipeline pipeline;
    private final SubtitleService subtitleService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VideoResponse> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid CreateVideoRequest request) {
        return ApiResponse.ok(videoService.uploadVideo(file, request));
    }

    @GetMapping
    public ApiResponse<Page<VideoResponse>> listAll(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(videoService.listAllVideos(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoResponse> getVideo(@PathVariable UUID id) {
        return ApiResponse.ok(videoService.getVideo(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<VideoResponse> updateVideo(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateVideoRequest request) {
        return ApiResponse.ok(videoService.updateVideo(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVideo(@PathVariable UUID id) {
        videoService.deleteVideo(id);
    }

    @PostMapping("/{id}/process")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<VideoResponse> processVideo(@PathVariable UUID id) {
        VideoResponse current = videoService.validateForProcessing(id);
        pipeline.processVideo(id);
        return ApiResponse.ok(current, "Processing started");
    }

    @GetMapping("/{id}/status")
    public ApiResponse<VideoStatusResponse> getStatus(@PathVariable UUID id) {
        return ApiResponse.ok(videoService.getVideoStatus(id));
    }

    @GetMapping("/{id}/subtitles")
    public ApiResponse<List<SubtitleSegmentResponse>> getSubtitles(@PathVariable UUID id) {
        return ApiResponse.ok(subtitleService.getSegmentResponses(id));
    }
}
