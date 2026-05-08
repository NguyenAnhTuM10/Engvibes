package com.englishapp.video;

import com.englishapp.user.CEFRLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID>, JpaSpecificationExecutor<Video> {

    Page<Video> findByStatus(VideoStatus status, Pageable pageable);
    List<Video> findAllByStatus(VideoStatus status);

    Page<Video> findByStatusAndCefrLevel(VideoStatus status, CEFRLevel cefrLevel, Pageable pageable);

    Page<Video> findByStatusAndTopic(VideoStatus status, String topic, Pageable pageable);

    Page<Video> findByStatusAndCefrLevelAndTopic(VideoStatus status, CEFRLevel cefrLevel, String topic, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status AND (LOWER(v.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(v.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Video> searchByStatusAndQuery(@Param("status") VideoStatus status, @Param("q") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}
