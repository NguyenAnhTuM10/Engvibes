package com.englishapp.sm2;

import com.englishapp.common.ApiException;
import com.englishapp.sm2.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Sm2Service {

    private final Sm2DeckRepository   deckRepo;
    private final Sm2CardRepository   cardRepo;
    private final Sm2ReviewRepository reviewRepo;
    private final Sm2Scheduler        scheduler;

    // ── Decks ─────────────────────────────────────────────────────────────

    @Transactional
    public Sm2Deck createDeck(DeckRequest req) {
        return deckRepo.save(Sm2Deck.builder()
                .name(req.name())
                .description(req.description())
                .build());
    }

    public List<Sm2Deck> listDecks() {
        return deckRepo.findAll();
    }

    public Sm2Deck getDeck(UUID id) {
        return deckRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Deck not found"));
    }

    // ── Cards ──────────────────────────────────────────────────────────────

    @Transactional
    public Sm2Card createCard(CardRequest req) {
        if (!deckRepo.existsById(req.deckId())) {
            throw ApiException.notFound("Deck not found");
        }
        return cardRepo.save(Sm2Card.builder()
                .deckId(req.deckId())
                .front(req.front())
                .back(req.back())
                .ipa(req.ipa())
                .exampleSentence(req.exampleSentence())
                .build());
    }

    public List<Sm2Card> listCards(UUID deckId) {
        return cardRepo.findByDeckId(deckId);
    }

    // ── Review queue ───────────────────────────────────────────────────────

    /**
     * Trả card quá hạn + card mới, sắp xếp theo due_date tăng dần.
     * deckId null = tất cả deck.
     */
    public List<QueueItem> getQueue(UUID deckId, int limit) {
        List<Object[]> rows = reviewRepo.findQueue(deckId, Instant.now(), limit);
        return rows.stream().map(this::toQueueItem).toList();
    }

    // ── Review ─────────────────────────────────────────────────────────────

    /**
     * Chấm 1 lần ôn: chạy SM-2, lưu kết quả, trả ReviewResult.
     */
    @Transactional
    public ReviewResult review(UUID cardId, int quality) {
        if (!cardRepo.existsById(cardId)) {
            throw ApiException.notFound("Card not found");
        }
        // Lấy hoặc tạo mới review record
        Sm2Review rev = reviewRepo.findByCardId(cardId)
                .orElseGet(() -> reviewRepo.save(
                        Sm2Review.builder().cardId(cardId).build()));

        scheduler.schedule(rev, quality);
        reviewRepo.save(rev);

        return new ReviewResult(
                cardId, quality,
                rev.getRepetitions(), rev.getIntervalDays(),
                round2(rev.getEaseFactor()), rev.getDueDate(), rev.getLastReviewed());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private QueueItem toQueueItem(Object[] row) {
        // Native query column order: card_id, deck_id, front, back, ipa,
        //   example_sentence, ease_factor, interval_days, repetitions,
        //   due_date, last_reviewed
        UUID    cardId   = toUUID(row[0]);
        UUID    deckId   = toUUID(row[1]);
        boolean isNew    = row[6] == null; // ease_factor null → no review row

        return new QueueItem(
                cardId,
                deckId,
                str(row[2]),
                str(row[3]),
                str(row[4]),
                str(row[5]),
                isNew,
                isNew ? null : toInt(row[8]),
                isNew ? null : toInt(row[7]),
                isNew ? null : toDouble(row[6]),
                isNew ? null : toInstant(row[9]),
                isNew ? null : toInstant(row[10])
        );
    }

    private UUID    toUUID(Object o)    { return o == null ? null : UUID.fromString(o.toString()); }
    private String  str(Object o)       { return o == null ? null : o.toString(); }
    private int     toInt(Object o)     { return o == null ? 0 : ((Number) o).intValue(); }
    private double  toDouble(Object o)  { return o == null ? 2.5 : ((Number) o).doubleValue(); }
    private double  round2(double v)    { return Math.round(v * 100.0) / 100.0; }
    private Instant toInstant(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return Instant.parse(o.toString());
    }
}
