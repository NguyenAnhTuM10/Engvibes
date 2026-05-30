package com.englishapp.sm2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Sm2ReviewRepository extends JpaRepository<Sm2Review, UUID> {

    Optional<Sm2Review> findByCardId(UUID cardId);

    /**
     * Review queue: trả card quá hạn + card mới (chưa có review).
     * Sort: due_date tăng dần — card quá hạn lâu nhất lên trước.
     * New cards (review IS NULL) dùng epoch làm due_date ảo → sort cuối overdue, trước future.
     */
    @Query(value = """
            SELECT c.id        AS card_id,
                   c.deck_id,
                   c.front,
                   c.back,
                   c.ipa,
                   c.example_sentence,
                   r.ease_factor,
                   r.interval_days,
                   r.repetitions,
                   r.due_date,
                   r.last_reviewed
            FROM   sm2_cards c
            LEFT JOIN sm2_reviews r ON r.card_id = c.id
            WHERE  (:deckId IS NULL OR c.deck_id = :deckId)
            AND    (r.id IS NULL OR r.due_date <= :now)
            ORDER BY COALESCE(r.due_date, TIMESTAMP '1970-01-01') ASC
            LIMIT  :lim
            """, nativeQuery = true)
    List<Object[]> findQueue(@Param("deckId") UUID deckId,
                             @Param("now") Instant now,
                             @Param("lim") int lim);
}
