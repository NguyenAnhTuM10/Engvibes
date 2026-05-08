package com.englishapp.recommend;

import com.englishapp.user.CEFRLevel;
import com.englishapp.video.Video;
import com.englishapp.video.VideoRepository;
import com.englishapp.video.VideoService;
import com.englishapp.video.VideoStatus;
import com.englishapp.video.dto.VideoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContentBasedRecommenderTest {

    @Mock UserFeatureService userFeatureService;
    @Mock VideoRepository videoRepository;
    @Mock UserVideoInteractionRepository interactionRepository;
    @Mock VideoService videoService;

    @InjectMocks ContentBasedRecommender recommender;

    private UUID userId;
    private Video videoB1, videoB2, videoA2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        videoB1 = Video.builder()
                .id(UUID.randomUUID()).status(VideoStatus.PUBLISHED)
                .cefrLevel(CEFRLevel.B1).topic("business").viewCount(10).build();
        videoB2 = Video.builder()
                .id(UUID.randomUUID()).status(VideoStatus.PUBLISHED)
                .cefrLevel(CEFRLevel.B2).topic("science").viewCount(5).build();
        videoA2 = Video.builder()
                .id(UUID.randomUUID()).status(VideoStatus.PUBLISHED)
                .cefrLevel(CEFRLevel.A2).topic("travel").viewCount(1).build();
    }

    @Test
    void shouldPrioritizeMatchingCEFR() {
        UserFeatureVector user = new UserFeatureVector(
                CEFRLevel.B1, List.of(), List.of(), Map.of(), 3);
        given(userFeatureService.buildUserFeatureVector(userId)).willReturn(user);
        given(videoRepository.findAllByStatus(VideoStatus.PUBLISHED))
                .willReturn(List.of(videoB1, videoB2, videoA2));
        given(interactionRepository.findCompletedVideoIdsByUserId(userId)).willReturn(Set.of());
        given(interactionRepository.findAllVideoIdsByUserId(userId)).willReturn(Set.of());
        given(videoService.toVideoResponse(any())).willAnswer(inv -> {
            Video v = inv.getArgument(0);
            return VideoResponse.builder().id(v.getId()).cefrLevel(v.getCefrLevel()).build();
        });

        List<VideoResponse> results = recommender.recommend(userId, 3);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getCefrLevel()).isEqualTo(CEFRLevel.B1);
    }

    @Test
    void shouldExcludeCompletedVideos() {
        UserFeatureVector user = new UserFeatureVector(
                CEFRLevel.B1, List.of(), List.of(), Map.of(), 2);
        given(userFeatureService.buildUserFeatureVector(userId)).willReturn(user);
        given(videoRepository.findAllByStatus(VideoStatus.PUBLISHED))
                .willReturn(List.of(videoB1, videoB2, videoA2));
        given(interactionRepository.findCompletedVideoIdsByUserId(userId))
                .willReturn(Set.of(videoB1.getId()));
        given(interactionRepository.findAllVideoIdsByUserId(userId)).willReturn(Set.of(videoB1.getId()));
        given(videoService.toVideoResponse(any())).willAnswer(inv -> {
            Video v = inv.getArgument(0);
            return VideoResponse.builder().id(v.getId()).build();
        });

        List<VideoResponse> results = recommender.recommend(userId, 10);

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(VideoResponse::getId))
                .doesNotContain(videoB1.getId());
    }

    @Test
    void shouldFavorNewTopicOverRecentTopic() {
        // Both B1 videos — only topic diversity differentiates them
        Video videoB1Business = Video.builder()
                .id(UUID.randomUUID()).status(VideoStatus.PUBLISHED)
                .cefrLevel(CEFRLevel.B1).topic("business").viewCount(5).build();
        Video videoB1Travel = Video.builder()
                .id(UUID.randomUUID()).status(VideoStatus.PUBLISHED)
                .cefrLevel(CEFRLevel.B1).topic("travel").viewCount(5).build();

        // User recently watched "business" 3 times → just watched
        UserFeatureVector user = new UserFeatureVector(
                CEFRLevel.B1, List.of(), List.of("business", "business", "business"), Map.of(), 5);
        given(userFeatureService.buildUserFeatureVector(userId)).willReturn(user);
        given(videoRepository.findAllByStatus(VideoStatus.PUBLISHED))
                .willReturn(List.of(videoB1Business, videoB1Travel));
        given(interactionRepository.findCompletedVideoIdsByUserId(userId)).willReturn(Set.of());
        given(interactionRepository.findAllVideoIdsByUserId(userId)).willReturn(Set.of());
        given(videoService.toVideoResponse(any())).willAnswer(inv -> {
            Video v = inv.getArgument(0);
            return VideoResponse.builder().id(v.getId()).topic(v.getTopic()).build();
        });

        List<VideoResponse> results = recommender.recommend(userId, 2);

        // travel is a new topic (score 1.0) > business just-watched (score 0.3)
        assertThat(results.get(0).getTopic()).isEqualTo("travel");
    }
}
