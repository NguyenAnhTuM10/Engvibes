package com.englishapp.video;

import com.englishapp.common.ApiResponse;
import com.englishapp.user.CEFRLevel;
import com.englishapp.video.dto.VideoFilter;
import com.englishapp.video.dto.VideoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ApiResponse<Page<VideoResponse>> listVideos(
            @RequestParam(required = false) CEFRLevel cefr,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        VideoFilter filter = new VideoFilter();
        filter.setCefrLevel(cefr);
        filter.setTopic(topic);
        filter.setSearch(search);
        filter.setStatus(VideoStatus.PUBLISHED);

        return ApiResponse.ok(videoService.listVideos(filter, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoResponse> getVideo(@PathVariable UUID id) {
        return ApiResponse.ok(videoService.getPublishedVideo(id));
    }
}
