package com.englishapp.shadow;

import java.util.ArrayList;
import java.util.List;

public final class WordDiffUtil {

    private WordDiffUtil() {}

    public static List<WordMatch> diff(String original, String spoken) {
        String[] exp = normalizeWords(original);
        String[] spk = normalizeWords(spoken);

        List<WordMatch> result = new ArrayList<>();
        int j = 0;

        for (String expected : exp) {
            if (j >= spk.length) {
                result.add(new WordMatch(expected, null, WordMatch.Status.MISSING));
            } else if (expected.equals(spk[j])) {
                result.add(new WordMatch(expected, spk[j++], WordMatch.Status.MATCH));
            } else if (levenshtein(expected, spk[j]) <= 2) {
                result.add(new WordMatch(expected, spk[j++], WordMatch.Status.MISPRONOUNCED));
            } else {
                result.add(new WordMatch(expected, null, WordMatch.Status.MISSING));
            }
        }
        while (j < spk.length) {
            result.add(new WordMatch(null, spk[j++], WordMatch.Status.EXTRA));
        }
        return result;
    }

    public static double accuracy(List<WordMatch> diff) {
        long expected = diff.stream()
                .filter(w -> w.status() != WordMatch.Status.EXTRA)
                .count();
        if (expected == 0) return 0.0;
        long matched = diff.stream()
                .filter(w -> w.status() == WordMatch.Status.MATCH)
                .count();
        return (double) matched / expected;
    }

    private static String[] normalizeWords(String text) {
        String cleaned = text.toLowerCase().replaceAll("[^a-z\\s]", "").trim();
        if (cleaned.isEmpty()) return new String[0];
        return cleaned.split("\\s+");
    }

    private static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }
}
