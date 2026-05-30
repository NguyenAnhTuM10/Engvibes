package com.englishapp.game;

import com.englishapp.common.ApiException;
import com.englishapp.game.dto.*;
import com.englishapp.sm2.Sm2Card;
import com.englishapp.sm2.Sm2CardRepository;
import com.englishapp.sm2.Sm2Service;
import com.englishapp.sm2.dto.ReviewResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final int MAX_QUESTIONS = 50;
    private static final int MC_OPTIONS    = 4;

    private final Sm2CardRepository cardRepo;
    private final Sm2Service        sm2Service;   // tái sử dụng SM-2, không copy

    // ─────────────────────────────────────────────────────────────────────
    // 1. Multiple Choice
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sinh N câu multiple choice từ deck.
     * Mỗi câu: front là từ hỏi, 4 options gồm 1 đáp án đúng + 3 nhiễu ngẫu nhiên
     * lấy từ back của card KHÁC trong cùng deck. Đáp án không lộ.
     */
    public List<MultipleChoiceQuestion> generateMultipleChoice(UUID deckId, int count) {
        List<Sm2Card> all = requireDeckCards(deckId, MC_OPTIONS);
        int n = Math.min(count, Math.min(all.size(), MAX_QUESTIONS));

        // Xáo trộn + lấy n câu
        List<Sm2Card> questions = new ArrayList<>(all);
        Collections.shuffle(questions);
        questions = questions.subList(0, n);

        // Tập distractors: tất cả back values trong deck
        List<String> allBacks = all.stream().map(Sm2Card::getBack).toList();

        List<MultipleChoiceQuestion> result = new ArrayList<>();
        for (Sm2Card q : questions) {
            List<String> distractors = allBacks.stream()
                    .filter(b -> !b.equals(q.getBack()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            Collections.shuffle(distractors);

            List<String> options = new ArrayList<>();
            options.add(q.getBack());                               // correct
            options.addAll(distractors.subList(0, MC_OPTIONS - 1)); // 3 distractors
            Collections.shuffle(options);

            result.add(new MultipleChoiceQuestion(q.getId(), q.getFront(), options));
        }
        return result;
    }

    /**
     * Chấm câu trả lời multiple choice.
     * Đáp án đúng lấy từ DB (không tin client).
     * Map: correct → quality=4, wrong → quality=2, gọi SM-2.
     */
    public AnswerResult checkAnswer(UUID cardId, String selected) {
        Sm2Card card = requireCard(cardId);
        boolean correct = card.getBack().equalsIgnoreCase(selected.trim());
        int quality = correct ? 4 : 2;

        ReviewResult rev = sm2Service.review(cardId, quality);  // ← tái sử dụng
        return new AnswerResult(correct, card.getBack(),
                cardId, rev.repetitions(), rev.intervalDays(),
                rev.easeFactor(), rev.dueDate());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Matching
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sinh game matching: 2 list xáo trộn riêng biệt.
     * Terms có cardId để client gửi lại khi chấm.
     * Definitions chỉ là chuỗi — không lộ mapping.
     */
    public MatchingGame generateMatching(UUID deckId, int count) {
        List<Sm2Card> all = requireDeckCards(deckId, 2);
        int n = Math.min(count, Math.min(all.size(), MAX_QUESTIONS));

        List<Sm2Card> cards = new ArrayList<>(all);
        Collections.shuffle(cards);
        cards = cards.subList(0, n);

        List<MatchingGame.Term> terms = cards.stream()
                .map(c -> new MatchingGame.Term(c.getId(), c.getFront()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        List<String> definitions = cards.stream()
                .map(Sm2Card::getBack)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        // Xáo trộn 2 list độc lập
        Collections.shuffle(terms);
        Collections.shuffle(definitions);

        return new MatchingGame(terms, definitions);
    }

    /**
     * Chấm matching: với mỗi (cardId, matchedBack), tra DB để biết đáp án đúng.
     * Không cập nhật SM-2 ở đây — matching không map 1-1 sang quality rõ ràng.
     */
    public MatchingCheckResult checkMatching(List<MatchingCheckRequest.Pair> pairs) {
        int correct = 0;
        List<MatchingCheckResult.Wrong> wrong = new ArrayList<>();

        for (MatchingCheckRequest.Pair p : pairs) {
            Sm2Card card = requireCard(p.cardId());
            if (card.getBack().equalsIgnoreCase(p.matchedBack().trim())) {
                correct++;
            } else {
                wrong.add(new MatchingCheckResult.Wrong(
                        card.getId(), card.getFront(),
                        p.matchedBack(), card.getBack()));
            }
        }
        return new MatchingCheckResult(correct, pairs.size(), wrong);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Typing Recall
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sinh câu hỏi typing: hiện back (nghĩa), user gõ front (từ).
     */
    public List<TypingQuestion> generateTyping(UUID deckId, int count) {
        List<Sm2Card> all = requireDeckCards(deckId, 1);
        int n = Math.min(count, Math.min(all.size(), MAX_QUESTIONS));

        List<Sm2Card> cards = new ArrayList<>(all);
        Collections.shuffle(cards);

        return cards.subList(0, n).stream()
                .map(c -> new TypingQuestion(c.getId(), c.getBack()))
                .toList();
    }

    /**
     * Chấm typing: so khớp case-insensitive + trim.
     * Levenshtein distance ≤ 1 → tính là đúng (typo nhẹ).
     * Cập nhật SM-2: correct → quality=4, wrong → quality=2.
     */
    public TypingCheckResult checkTyping(UUID cardId, String typed) {
        Sm2Card card = requireCard(cardId);
        String expected = card.getFront().trim().toLowerCase();
        String answer   = (typed == null ? "" : typed.trim().toLowerCase());

        int dist = levenshtein(expected, answer);
        boolean correct = dist <= 1;
        int quality = correct ? 4 : 2;

        ReviewResult rev = sm2Service.review(cardId, quality);  // ← tái sử dụng
        return new TypingCheckResult(correct, card.getFront(), dist,
                cardId, rev.repetitions(), rev.intervalDays(),
                rev.easeFactor(), rev.dueDate());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Levenshtein distance — O(mn)
    // ─────────────────────────────────────────────────────────────────────

    static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[m][n];
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private List<Sm2Card> requireDeckCards(UUID deckId, int minCards) {
        List<Sm2Card> cards = cardRepo.findByDeckId(deckId);
        if (cards.size() < minCards) {
            throw ApiException.badRequest(
                    "Deck needs at least " + minCards + " card(s), has " + cards.size());
        }
        return cards;
    }

    private Sm2Card requireCard(UUID cardId) {
        return cardRepo.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found: " + cardId));
    }
}
