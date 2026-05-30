package com.englishapp.sm2;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái SRS (SM-2) của 1 card.
 *
 * Fields:
 *   easeFactor   — EF (Ease Factor): nhân tử tăng interval. Khởi đầu 2.5, min 1.3.
 *                  EF cao → card dễ → interval tăng nhanh hơn.
 *   intervalDays — Số ngày đến lần ôn tiếp theo.
 *   repetitions  — Số lần trả lời đúng liên tiếp. Reset về 0 khi sai (q < 3).
 *   dueDate      — Thời điểm ôn tiếp theo.
 *   lastReviewed — Null nếu chưa ôn lần nào (card mới).
 */
@Entity
@Table(name = "sm2_reviews")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Sm2Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID cardId;

    @Column(nullable = false)
    @Builder.Default
    private double easeFactor = 2.5;

    @Column(nullable = false)
    @Builder.Default
    private int intervalDays = 0;

    @Column(nullable = false)
    @Builder.Default
    private int repetitions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Instant dueDate = Instant.now();

    private Instant lastReviewed;   // null = chưa ôn
}
