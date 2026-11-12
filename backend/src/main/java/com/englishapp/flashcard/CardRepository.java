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

    @Query(value = "SELECT c FROM UserCard c LEFT JOIN FETCH c.vocab WHERE c.deckId = :deckId ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM UserCard c WHERE c.deckId = :deckId")
    Page<UserCard> findByDeckId(@Param("deckId") UUID deckId, Pageable pageable);

    /** Card đến hạn trong deck, quá hạn lâu nhất lên trước. */
    @Query("SELECT c FROM UserCard c LEFT JOIN FETCH c.vocab "
         + "WHERE c.deckId = :deckId AND c.nextReview <= :now ORDER BY c.nextReview ASC")
    List<UserCard> findDueInDeck(@Param("deckId") UUID deckId, @Param("now") Instant now, Pageable pageable);

    /** Toàn bộ card trong deck kèm vocab — dùng cho games (tránh N+1). */
    @Query("SELECT c FROM UserCard c LEFT JOIN FETCH c.vocab WHERE c.deckId = :deckId")
    List<UserCard> findAllInDeckWithVocab(@Param("deckId") UUID deckId);

    /** Mặt trước (đã lower-case) của mọi card trong deck — dùng để chống trùng khi import. */
    @Query("SELECT LOWER(COALESCE(v.word, c.front)) FROM UserCard c LEFT JOIN c.vocab v WHERE c.deckId = :deckId")
    List<String> findFrontsLowerInDeck(@Param("deckId") UUID deckId);

    Optional<UserCard> findByUserIdAndVocab_IdAndDeckId(UUID userId, UUID vocabId, UUID deckId);

    Optional<UserCard> findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(UUID deckId, String front);

    long countByDeckId(UUID deckId);
    long countByDeckIdAndNextReviewLessThanEqual(UUID deckId, Instant now);
    long countByUserIdAndStateNot(UUID userId, CardState state);

    @Query("SELECT c FROM UserCard c LEFT JOIN FETCH c.vocab WHERE c.userId = :userId AND c.sourceVideoId = :sourceVideoId")
    List<UserCard> findByUserIdAndSourceVideoId(@Param("userId") UUID userId, @Param("sourceVideoId") UUID sourceVideoId);

    /** Chỉ card từ điển (inner join) — recommend cần phoneme / CEFR của vocab. */
    @Query("SELECT c FROM UserCard c JOIN FETCH c.vocab WHERE c.userId = :userId AND c.nextReview <= :now")
    List<UserCard> findDueByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT c.vocab.cefrLevel, COUNT(c) FROM UserCard c WHERE c.userId = :userId GROUP BY c.vocab.cefrLevel")
    List<Object[]> countByUserIdGroupByCefrLevel(@Param("userId") UUID userId);

    long countByUserIdAndStabilityGreaterThan(UUID userId, double stability);

    @Query("SELECT c.createdAt, c.vocab.cefrLevel FROM UserCard c WHERE c.userId = :userId ORDER BY c.createdAt")
    List<Object[]> findCreatedAtAndCefrByUserId(@Param("userId") UUID userId);
}
