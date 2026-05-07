package com.englishapp.flashcard;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Simplified FSRS-4.5 spaced repetition scheduler.
 * Ratings: 1=Again, 2=Hard, 3=Good, 4=Easy
 */
@Component
public class FsrsScheduler {

    // FSRS-4.5 default weights
    private static final double[] W = {
        0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0589,
        1.5330, 0.1544, 0.9956, 1.9975, 0.1100, 0.2900, 2.2700, 0.1350, 2.9898
    };

    private static final double TARGET_RETENTION = 0.9;
    // interval = stability * -ln(targetRetention) = stability * ln(1/0.9)
    private static final double RETENTION_FACTOR = -Math.log(TARGET_RETENTION);

    public void review(UserCard card, int rating) {
        Instant now = Instant.now();
        card.setReviewCount(card.getReviewCount() + 1);

        switch (card.getState()) {
            case NEW       -> scheduleNew(card, rating, now);
            case LEARNING, RELEARNING -> scheduleLearning(card, rating, now);
            case REVIEW    -> scheduleReview(card, rating, now);
        }

        card.setLastReview(now);
    }

    private void scheduleNew(UserCard card, int rating, Instant now) {
        card.setStability(Math.max(0.1, W[rating - 1]));
        card.setDifficulty(clampD(W[4] - Math.exp(W[5] * (rating - 1)) + 1));

        if (rating <= 2) {
            card.setState(CardState.LEARNING);
            card.setNextReview(now.plusSeconds(rating == 1 ? 60 : 300));
        } else {
            card.setState(CardState.REVIEW);
            card.setNextReview(daysFromNow(card.getStability(), now));
        }
    }

    private void scheduleLearning(UserCard card, int rating, Instant now) {
        if (rating <= 2) {
            card.setState(CardState.LEARNING);
            card.setNextReview(now.plusSeconds(rating == 1 ? 60 : 300));
        } else {
            card.setState(CardState.REVIEW);
            card.setNextReview(daysFromNow(card.getStability(), now));
        }
    }

    private void scheduleReview(UserCard card, int rating, Instant now) {
        Instant last = card.getLastReview();
        double elapsed = last != null
            ? Math.max(0.001, (now.toEpochMilli() - last.toEpochMilli()) / 86400000.0)
            : 1.0;

        double R = Math.min(1.0, Math.pow(TARGET_RETENTION, elapsed / Math.max(0.1, card.getStability())));
        double D = card.getDifficulty();
        double S = card.getStability();

        if (rating == 1) {
            card.setState(CardState.RELEARNING);
            card.setLapseCount(card.getLapseCount() + 1);
            double sNew = W[11] * Math.pow(D, -W[12])
                * (Math.pow(S + 1, W[13]) - 1)
                * Math.exp((1 - R) * W[14]);
            card.setStability(Math.max(0.1, sNew));
            card.setNextReview(now.plusSeconds(600));
        } else {
            double hardPenalty = (rating == 2) ? W[15] : 1.0;
            double easyBonus  = (rating == 4) ? W[16] : 1.0;
            double sNew = S * (Math.exp(W[8]) * (11 - D)
                * Math.pow(S, -W[9])
                * (Math.exp(W[10] * (1 - R)) - 1)
                * hardPenalty * easyBonus + 1);
            card.setStability(Math.max(0.1, sNew));
            card.setState(CardState.REVIEW);
            card.setNextReview(daysFromNow(card.getStability(), now));
        }

        // Difficulty update with mean reversion toward D0(rating=3)
        double d0 = clampD(W[4] - Math.exp(W[5] * 2) + 1);
        double dNew = W[7] * d0 + (1 - W[7]) * (D - W[6] * (rating - 3));
        card.setDifficulty(clampD(dNew));
    }

    private Instant daysFromNow(double stability, Instant now) {
        long days = Math.max(1L, Math.round(stability * RETENTION_FACTOR));
        return now.plusSeconds(days * 86400L);
    }

    private double clampD(double d) {
        return Math.max(1.0, Math.min(10.0, d));
    }
}
