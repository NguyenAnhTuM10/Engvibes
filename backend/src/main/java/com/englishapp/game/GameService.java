package com.englishapp.game;

import com.englishapp.common.ApiException;
import com.englishapp.flashcard.CardRepository;
import com.englishapp.flashcard.CardService;
import com.englishapp.flashcard.DeckService;
import com.englishapp.flashcard.UserCard;
import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.game.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mini-games ôn từ vựng trên deck FSRS của user.
 * Kết quả game được ghi ngược vào lịch ôn FSRS: đúng → Good (3), sai → Again (1).
 * Đáp án đúng luôn lấy từ DB — không tin dữ liệu client gửi lên.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    static final int RATING_CORRECT = 3;   // FSRS Good
    static final int RATING_WRONG   = 1;   // FSRS Again

    private static final int MAX_QUESTIONS = 50;
    private static final int MC_OPTIONS    = 4;

    private final DeckService    deckService;
    private final CardService    cardService;
    private final CardRepository cardRepository;

    // ─────────────────────────────────────────────────────────────────────
    // 1. Multiple Choice
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sinh N câu multiple choice từ deck.
     * Mỗi câu: front là từ hỏi, 4 options gồm 1 đáp án đúng + 3 nhiễu ngẫu nhiên
     * lấy từ back của card KHÁC trong cùng deck. Đáp án không lộ.
     */
    public List<MultipleChoiceQuestion> generateMultipleChoice(UUID deckId, int count, UUID userId) {
        List<CardFace> all = requireDeckCards(deckId, userId, MC_OPTIONS);
        List<CardFace> questions = pickRandom(all, count);

        // Tập distractors: mọi nghĩa khác nhau trong deck
        List<String> allBacks = all.stream().map(CardFace::back).distinct().toList();

        List<MultipleChoiceQuestion> result = new ArrayList<>();
        for (CardFace q : questions) {
            List<String> distractors = allBacks.stream()
                    .filter(b -> !b.equalsIgnoreCase(q.back()))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(distractors);

            List<String> options = new ArrayList<>();
            options.add(q.back());                                                   // correct
            options.addAll(distractors.subList(0, Math.min(MC_OPTIONS - 1, distractors.size())));
            Collections.shuffle(options);

            result.add(new MultipleChoiceQuestion(q.cardId(), q.front(), options));
        }
        return result;
    }

    @Transactional
    public AnswerResult checkAnswer(UUID cardId, String selected, UUID userId) {
        UserCard card = cardService.requireOwnedCard(cardId, userId);
        String expected = card.resolvedBack();
        boolean correct = selected != null && expected.equalsIgnoreCase(selected.trim());

        CardResponse after = cardService.reviewCard(cardId, correct ? RATING_CORRECT : RATING_WRONG, userId);
        return new AnswerResult(correct, expected, cardId, after.getState(), after.getNextReview());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Matching
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sinh game matching: 2 list xáo trộn riêng biệt.
     * Terms có cardId để client gửi lại khi chấm; definitions chỉ là chuỗi — không lộ mapping.
     */
    public MatchingGame generateMatching(UUID deckId, int count, UUID userId) {
        List<CardFace> cards = pickRandom(requireDeckCards(deckId, userId, 2), count);

        List<MatchingGame.Term> terms = cards.stream()
                .map(c -> new MatchingGame.Term(c.cardId(), c.front()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> definitions = cards.stream()
                .map(CardFace::back)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(terms);
        Collections.shuffle(definitions);
        return new MatchingGame(terms, definitions);
    }

    /**
     * Chấm matching. Không cập nhật FSRS — một lần ghép không map rõ ràng sang một mức đánh giá.
     */
    public MatchingCheckResult checkMatching(List<MatchingCheckRequest.Pair> pairs, UUID userId) {
        if (pairs == null || pairs.isEmpty()) {
            return new MatchingCheckResult(0, 0, List.of());
        }
        int correct = 0;
        List<MatchingCheckResult.Wrong> wrong = new ArrayList<>();

        for (MatchingCheckRequest.Pair p : pairs) {
            UserCard card = cardService.requireOwnedCard(p.cardId(), userId);
            String expected = card.resolvedBack();
            String answer = p.matchedBack() == null ? "" : p.matchedBack().trim();
            if (expected.equalsIgnoreCase(answer)) {
                correct++;
            } else {
                wrong.add(new MatchingCheckResult.Wrong(card.getId(), card.resolvedFront(), answer, expected));
            }
        }
        return new MatchingCheckResult(correct, pairs.size(), wrong);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Typing Recall
    // ─────────────────────────────────────────────────────────────────────

    /** Sinh câu hỏi typing: hiện back (nghĩa), user gõ front (từ). */
    public List<TypingQuestion> generateTyping(UUID deckId, int count, UUID userId) {
        return pickRandom(requireDeckCards(deckId, userId, 1), count).stream()
                .map(c -> new TypingQuestion(c.cardId(), c.back()))
                .toList();
    }

    /**
     * Chấm typing: so khớp không phân biệt hoa thường, Levenshtein ≤ 1 vẫn tính đúng (typo nhẹ).
     */
    @Transactional
    public TypingCheckResult checkTyping(UUID cardId, String typed, UUID userId) {
        UserCard card = cardService.requireOwnedCard(cardId, userId);
        String expectedRaw = card.resolvedFront();
        String expected = expectedRaw.trim().toLowerCase();
        String answer   = typed == null ? "" : typed.trim().toLowerCase();

        int dist = levenshtein(expected, answer);
        boolean correct = dist <= 1;

        CardResponse after = cardService.reviewCard(cardId, correct ? RATING_CORRECT : RATING_WRONG, userId);
        return new TypingCheckResult(correct, expectedRaw, dist, cardId, after.getState(), after.getNextReview());
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

    /** Nội dung đã resolve của 1 card — đọc trong transaction vì vocab là LAZY. */
    private record CardFace(UUID cardId, String front, String back) {}

    private List<CardFace> requireDeckCards(UUID deckId, UUID userId, int minCards) {
        deckService.requireOwnedDeck(deckId, userId);
        List<CardFace> cards = cardRepository.findAllInDeckWithVocab(deckId).stream()
                .map(c -> new CardFace(c.getId(), c.resolvedFront(), c.resolvedBack()))
                .filter(c -> c.front() != null && c.back() != null)
                .toList();
        if (cards.size() < minCards) {
            throw ApiException.badRequest(
                    "Deck needs at least " + minCards + " card(s), has " + cards.size());
        }
        return cards;
    }

    private static List<CardFace> pickRandom(List<CardFace> all, int count) {
        int n = Math.max(1, Math.min(count, Math.min(all.size(), MAX_QUESTIONS)));
        List<CardFace> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, n);
    }
}
