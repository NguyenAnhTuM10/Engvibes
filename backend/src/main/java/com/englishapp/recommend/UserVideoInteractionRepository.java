package com.englishapp.recommend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserVideoInteractionRepository extends JpaRepository<UserVideoInteraction, UserVideoInteractionId> {

    List<UserVideoInteraction> findByIdUserId(UUID userId);

    @Query("SELECT i.id.videoId FROM UserVideoInteraction i WHERE i.id.userId = :userId")
    Set<UUID> findAllVideoIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT i.id.videoId FROM UserVideoInteraction i WHERE i.id.userId = :userId AND i.completionScore >= 1.0")
    Set<UUID> findCompletedVideoIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_video_interaction (user_id, video_id, view_count, completion_score, last_viewed)
            VALUES (:userId, :videoId, 1, :completionScore, :lastViewed)
            ON CONFLICT (user_id, video_id) DO UPDATE SET
                view_count = user_video_interaction.view_count + 1,
                completion_score = :completionScore,
                last_viewed = :lastViewed
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId,
                @Param("videoId") UUID videoId,
                @Param("completionScore") double completionScore,
                @Param("lastViewed") Instant lastViewed);
}
