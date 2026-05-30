package com.englishapp.video.subtitle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubtitleRepository extends JpaRepository<SubtitleSegment, UUID> {

    List<SubtitleSegment> findByVideoIdOrderByOrderIndex(UUID videoId);

    @Modifying
    @Query("DELETE FROM SubtitleSegment s WHERE s.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") UUID videoId);

    @Query(value = "SELECT * FROM subtitle_segments WHERE video_id IN (SELECT id FROM videos WHERE status = 'PUBLISHED') ORDER BY RANDOM() LIMIT 1",
           nativeQuery = true)
    Optional<SubtitleSegment> findRandomPublishedPhrase();

    /**
     * Video đã PUBLISHED và CÓ phụ đề — nguồn câu luyện phát âm.
     * Trả về: id, title, cefr_level, số câu (sentence_count).
     */
    @Query(value = """
            SELECT v.id AS id, v.title AS title, v.cefr_level AS cefr_level,
                   COUNT(s.id) AS sentence_count
            FROM   videos v
            JOIN   subtitle_segments s ON s.video_id = v.id
            WHERE  v.status = 'PUBLISHED'
            GROUP  BY v.id, v.title, v.cefr_level
            ORDER  BY v.title
            """, nativeQuery = true)
    List<Object[]> findPublishedVideosWithSubtitles();
}
