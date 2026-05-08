package com.englishapp.flashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<UserCard, UUID> {
    Page<UserCard> findByDeckId(UUID deckId, Pageable pageable);
    Page<UserCard> findByDeckIdAndNextReviewBefore(UUID deckId, Instant now, Pageable pageable);
    Optional<UserCard> findByUserIdAndVocab_IdAndDeckId(UUID userId, UUID vocabId, UUID deckId);
    long countByDeckId(UUID deckId);
    long countByDeckIdAndNextReviewBefore(UUID deckId, Instant now);
    long countByUserIdAndStateNot(UUID userId, CardState state);

    @Query("SELECT c FROM UserCard c JOIN FETCH c.vocab WHERE c.userId = :userId AND c.sourceVideoId = :sourceVideoId")
    List<UserCard> findByUserIdAndSourceVideoId(@Param("userId") UUID userId, @Param("sourceVideoId") UUID sourceVideoId);

    @Query("SELECT c FROM UserCard c JOIN FETCH c.vocab WHERE c.userId = :userId AND c.nextReview <= :now")
    List<UserCard> findDueByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT c.vocab.cefrLevel, COUNT(c) FROM UserCard c WHERE c.userId = :userId GROUP BY c.vocab.cefrLevel")
    List<Object[]> countByUserIdGroupByCefrLevel(@Param("userId") UUID userId);

    long countByUserIdAndStabilityGreaterThan(UUID userId, double stability);

    @Query("SELECT c.createdAt, c.vocab.cefrLevel FROM UserCard c WHERE c.userId = :userId ORDER BY c.createdAt")
    List<Object[]> findCreatedAtAndCefrByUserId(@Param("userId") UUID userId);
}
