package com.englishapp.sm2;

import com.englishapp.pronunciation.PronunciationContentService;
import com.englishapp.pronunciation.dto.AnalyzeResult;
import com.englishapp.pronunciation.dto.PronunciationWord;
import com.englishapp.pronunciation.dto.WordAnalysis;
import com.englishapp.sm2.dto.ReviewResult;
import com.englishapp.sm2.dto.SoundCardChange;
import com.englishapp.vocab.VocabEntry;
import com.englishapp.vocab.VocabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cầu nối Pronunciation ↔ SRS (SM-2).
 *
 * Vòng khép kín:
 *   phát âm yếu → tự thêm card vào deck "Sounds to practice" (due = now)
 *   → ôn theo SM-2 → phát âm lại đúng → SM-2 review thành công → interval giãn ra.
 *
 * TÁI SỬ DỤNG hoàn toàn model + thuật toán SM-2 ({@link Sm2Service#review}),
 * KHÔNG copy công thức.
 *
 * ── Tiêu chí (rõ ràng, deterministic) ──────────────────────────────────────
 *   Dùng PER-WORD score từ word_analyses (đúng badge logic FE: <60 đỏ, ≥80 xanh).
 *   Với mỗi từ trong kết quả /analyze:
 *     • score < WEAK_THRESHOLD (60)  → từ YẾU:
 *         - chưa có trong deck → THÊM card mới + review due=now           (ADDED)
 *         - đã có trong deck   → review SM-2 quality thấp (q<3) → reset,
 *                                 EF giảm, due về interval tối thiểu       (DEMOTED)
 *     • score ≥ STRONG_THRESHOLD (80) và ĐÃ có trong deck → review q=5    (PROMOTED)
 *       (phát âm đúng = 1 lần review thành công → interval giãn ra)
 *     • 60 ≤ score < 80 và đã có trong deck → review quality vừa (q=3/4)
 *     • từ chưa từng yếu (≥60) và chưa có trong deck → KHÔNG thêm.
 *
 *   Bỏ qua từ chức năng ngắn (a/the/to/of…) để không nhồi rác vào deck —
 *   chúng hay bị connected-speech reduction chứ không phải lỗi phát âm thật.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoundsToPracticeService {

    public static final String DECK_NAME = "Sounds to practice";
    private static final String DECK_DESC =
            "Hệ thống tự thêm những từ/âm bạn phát âm chưa tốt để ôn theo spaced repetition.";

    private static final int WEAK_THRESHOLD   = 60;   // < 60 = đỏ = yếu (đúng badge logic FE)
    private static final int STRONG_THRESHOLD = 80;   // ≥ 80 = xanh = đạt

    /** Từ chức năng ngắn — không auto-add (hay bị nuốt khi nói nối, không phải lỗi thật). */
    private static final Set<String> SKIP_WORDS = Set.of(
            "a", "an", "the", "to", "of", "for", "in", "on", "at", "by", "as",
            "or", "and", "but", "so", "is", "are", "was", "were", "be", "am",
            "do", "did", "it", "i", "my", "me", "we", "he", "she", "you", "your",
            "this", "that", "not", "no", "too", "two", "one");

    private final Sm2DeckRepository deckRepo;
    private final Sm2CardRepository cardRepo;
    private final Sm2ReviewRepository reviewRepo;
    private final Sm2Service sm2Service;                       // ← reuse SM-2 engine
    private final VocabRepository vocabRepo;
    private final PronunciationContentService contentService;

    // ── Deck (system, find-or-create) ───────────────────────────────────────

    /**
     * Lấy (hoặc tạo) deck hệ thống "Sounds to practice".
     * Demo: 1 deck global (ownerId = null). Production sẽ key theo ownerId.
     * Deck này không có endpoint xóa → coi như không cho xóa.
     */
    @Transactional
    public Sm2Deck getOrCreateDeck() {
        return deckRepo.findFirstByName(DECK_NAME)
                .orElseGet(() -> deckRepo.save(Sm2Deck.builder()
                        .name(DECK_NAME)
                        .description(DECK_DESC)
                        .build()));
    }

    public List<Sm2Card> listCards() {
        return deckRepo.findFirstByName(DECK_NAME)
                .map(d -> cardRepo.findByDeckId(d.getId()))
                .orElseGet(List::of);
    }

    // ── Link sau mỗi lần /analyze ────────────────────────────────────────────

    /**
     * Cập nhật deck "Sounds to practice" dựa trên kết quả phát âm.
     * Trả về danh sách thay đổi (cho FE hiển thị).
     * Gọi trong cùng transaction của saveAttemptResult.
     */
    @Transactional
    public List<SoundCardChange> linkAttempt(AnalyzeResult result) {
        List<SoundCardChange> changes = new ArrayList<>();
        List<WordAnalysis> words = result.wordAnalyses();
        if (words == null || words.isEmpty()) return changes;

        Optional<Sm2Deck> deckOpt = deckRepo.findFirstByName(DECK_NAME);

        for (WordAnalysis wa : words) {
            String word = wa.word() == null ? "" : wa.word().trim().toLowerCase();
            if (word.length() < 3 || SKIP_WORDS.contains(word)) continue;

            int score = wa.score();
            Optional<Sm2Card> cardOpt = deckOpt.flatMap(
                    d -> cardRepo.findFirstByDeckIdAndFrontIgnoreCase(d.getId(), word));

            if (score < WEAK_THRESHOLD) {
                if (cardOpt.isPresent()) {
                    // Yếu + đã có → review quality thấp (q<3): EF giảm, due về tối thiểu
                    changes.add(reviewExisting(cardOpt.get(), word, score, "DEMOTED"));
                } else {
                    // Yếu + chưa có → thêm card mới, due = now
                    Sm2Deck deck = deckOpt.orElseGet(this::getOrCreateDeck);
                    if (deckOpt.isEmpty()) deckOpt = Optional.of(deck);
                    changes.add(addCard(deck, word, wa, score));
                }
            } else if (cardOpt.isPresent()) {
                // Đã có trong deck & phát âm ổn/tốt → review thành công (q≥3, ≥80 → q=5)
                changes.add(reviewExisting(cardOpt.get(), word, score, "PROMOTED"));
            }
            // else: score ≥ 60 và chưa có trong deck → không thêm (chỉ thêm khi yếu)
        }
        return changes;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SoundCardChange addCard(Sm2Deck deck, String word, WordAnalysis wa, int score) {
        Optional<PronunciationWord> content = contentService.findWord(word);
        Optional<VocabEntry> vocab = vocabRepo.findFirstByWordIgnoreCase(word);

        String ipa = content.map(PronunciationWord::ipa)
                .or(() -> vocab.map(VocabEntry::getIpa))
                .orElse(wa.wordIpa());
        // back ưu tiên nghĩa tiếng Việt curated trong content, rồi vocab definition,
        // cuối cùng fallback mô tả âm cần luyện.
        String back = content.map(PronunciationWord::vi)
                .filter(s -> s != null && !s.isBlank())
                .or(() -> vocab.map(VocabEntry::getDefinition).filter(s -> s != null && !s.isBlank()))
                .or(() -> content.map(c -> "Luyện âm /" + c.targetSound() + "/"))
                .orElse("Luyện phát âm từ này");
        String example = content.map(PronunciationWord::exampleSentence).orElse(null);

        Sm2Card card = cardRepo.save(Sm2Card.builder()
                .deckId(deck.getId())
                .front(word)
                .back(back)
                .ipa(ipa)
                .exampleSentence(example)
                .build());

        // Review due = now → vào queue ngay (tái dùng default SRS: EF 2.5, interval 0, due now)
        Sm2Review review = reviewRepo.save(Sm2Review.builder()
                .cardId(card.getId())
                .dueDate(Instant.now())
                .build());

        log.info("[SoundsToPractice] ADDED '{}' (score={}) → card={}", word, score, card.getId());
        return new SoundCardChange(card.getId(), word, "ADDED", ipa, score,
                review.getIntervalDays(), review.getDueDate());
    }

    private SoundCardChange reviewExisting(Sm2Card card, String word, int score, String action) {
        // Tái dùng đúng SM-2 engine — KHÔNG copy công thức
        ReviewResult rr = sm2Service.review(card.getId(), scoreToQuality(score));
        log.info("[SoundsToPractice] {} '{}' (score={}, q={}) → interval={}d due={}",
                action, word, score, scoreToQuality(score), rr.intervalDays(), rr.dueDate());
        return new SoundCardChange(card.getId(), word, action, card.getIpa(), score,
                rr.intervalDays(), rr.dueDate());
    }

    /**
     * Map per-word score → SM-2 quality (0..5).
     *   ≥80 → 5 (perfect)   70..79 → 4   60..69 → 3 (đúng nhưng khó)
     *   40..59 → 2          <40 → 1  (q<3 = sai → SM-2 reset interval, giảm EF)
     */
    static int scoreToQuality(int score) {
        if (score >= 80) return 5;
        if (score >= 70) return 4;
        if (score >= 60) return 3;
        if (score >= 40) return 2;
        return 1;
    }
}
