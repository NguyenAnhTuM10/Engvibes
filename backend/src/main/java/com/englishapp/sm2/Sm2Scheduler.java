package com.englishapp.sm2;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Thuật toán SuperMemo 2 (SM-2).
 *
 * Nguồn tham khảo (verified):
 *   Wozniak, P. A. (1990). "Application of a computer to improve the results obtained
 *   in working with the SuperMemo method."
 *   https://www.supermemo.com/en/blog/application-of-a-computer-to-improve-the-results
 *   -obtained-in-working-with-the-supermemo-method
 *
 * Quality q ∈ 0..5:
 *   0 = complete blackout / quên hoàn toàn
 *   1 = incorrect, serious error
 *   2 = incorrect, correct answer seemed familiar
 *   3 = correct with significant difficulty
 *   4 = correct after hesitation
 *   5 = perfect response
 *
 * Demo mapping (4 nút):
 *   "Again" → q=1  |  "Hard" → q=3  |  "Good" → q=4  |  "Easy" → q=5
 *
 * Công thức:
 *   If q < 3:
 *     repetitions = 0
 *     interval    = 1
 *   Else (q >= 3):
 *     reps == 0 → interval = 1
 *     reps == 1 → interval = 6
 *     else      → interval = round(interval × EF)
 *     repetitions++
 *
 *   EF' = EF + (0.1 − (5−q) × (0.08 + (5−q) × 0.02))
 *   EF'  = max(1.3, EF')
 *   due  = now + interval days
 */
@Component
public class Sm2Scheduler {

    private static final double MIN_EF = 1.3;

    /**
     * Chạy 1 lần ôn, trả về review đã cập nhật (mutate in-place + return).
     *
     * @param review trạng thái SRS hiện tại
     * @param quality đánh giá của user (0–5)
     */
    public Sm2Review schedule(Sm2Review review, int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("quality must be 0–5, got: " + quality);
        }

        if (quality < 3) {
            // Sai → reset
            review.setRepetitions(0);
            review.setIntervalDays(1);
        } else {
            // Đúng → tăng interval theo số lần lặp
            int reps = review.getRepetitions();
            int newInterval = switch (reps) {
                case 0  -> 1;
                case 1  -> 6;
                default -> (int) Math.round(review.getIntervalDays() * review.getEaseFactor());
            };
            review.setIntervalDays(newInterval);
            review.setRepetitions(reps + 1);
        }

        // Cập nhật EF: giảm khi sai/khó, tăng khi dễ
        double ef = review.getEaseFactor()
                + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        review.setEaseFactor(Math.max(MIN_EF, ef));

        review.setDueDate(Instant.now().plus(review.getIntervalDays(), ChronoUnit.DAYS));
        review.setLastReviewed(Instant.now());

        return review;
    }
}
