package com.englishapp.video.subtitle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubtitleRepository extends JpaRepository<SubtitleSegment, UUID> {

    List<SubtitleSegment> findByVideoIdOrderByOrderIndex(UUID videoId);

    @Modifying
    @Query("DELETE FROM SubtitleSegment s WHERE s.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") UUID videoId);
}
