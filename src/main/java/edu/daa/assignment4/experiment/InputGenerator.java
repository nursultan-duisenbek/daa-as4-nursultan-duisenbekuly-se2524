package edu.daa.assignment4.experiment;

import java.util.Random;

/**
 * Builds every input of the experiment from a fixed seed: the same seed always gives the same strings.
 * Each method says what the input is meant to show.
 */
public final class InputGenerator {

    /** Two strings: (a, b) for edit distance, (text, pattern) for string search. */
    public record InputPair(String first, String second) {
    }

    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String FIRST_HALF = "abcdefghijklm";
    private static final String SECOND_HALF = "nopqrstuvwxyz";
    private static final String WITHOUT_Z = "abcdefghijklmnopqrstuvwxy";

    /** How many copies of the pattern are planted into the random search text. */
    public static final int PLANTED_COPIES = 5;

    private final Random random;

    public InputGenerator(long seed) {
        this.random = new Random(seed);
    }

    public String randomString(int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ edit distance (|a| = |b| = n)

    /**
     * Best case for the recursion: a and b are the same random string. Every call finds equal last
     * characters and follows the diagonal only, so the recursion makes n + 1 calls.
     */
    public InputPair editIdentical(int n) {
        String s = randomString(n, LOWERCASE);
        return new InputPair(s, s);
    }

    /**
     * Typical case: two independent random strings over a..z. About 1 character pair in 26 matches,
     * so the recursion is almost as busy as in the worst case.
     */
    public InputPair editRandom(int n) {
        return new InputPair(randomString(n, LOWERCASE), randomString(n, LOWERCASE));
    }

    /**
     * Worst case for the recursion: a only uses a..m and b only uses n..z, so no pair of characters
     * ever matches and every call branches three ways.
     */
    public InputPair editDisjoint(int n) {
        return new InputPair(randomString(n, FIRST_HALF), randomString(n, SECOND_HALF));
    }

    // ------------------------------------------------------------------ string search (|text| = n, |pattern| = m)

    /**
     * Best case for naive search: the text has no 'z' and the pattern starts with 'z', so every start
     * position fails on its first comparison. There is no match.
     */
    public InputPair searchBestCase(int n, int m) {
        check(n, m);
        String text = randomString(n, WITHOUT_Z);
        String pattern = "z" + randomString(m - 1, WITHOUT_Z);
        return new InputPair(text, pattern);
    }

    /**
     * Typical case: random text over a..z with a random pattern planted {@value #PLANTED_COPIES} times,
     * evenly spaced, so a real search finds something. Random text hardly ever has long partial matches.
     */
    public InputPair searchRandom(int n, int m) {
        check(n, m);
        int spacing = n / (PLANTED_COPIES + 1);
        if (spacing < m) {
            throw new IllegalArgumentException("The text is too short to plant " + PLANTED_COPIES
                    + " copies of a pattern of length " + m);
        }
        char[] text = randomString(n, LOWERCASE).toCharArray();
        String pattern = randomString(m, LOWERCASE);
        for (int k = 1; k <= PLANTED_COPIES; k++) {
            pattern.getChars(0, m, text, k * spacing);
        }
        return new InputPair(new String(text), pattern);
    }

    /**
     * Worst case for naive search: text a^n and pattern a^(m-1) b. Every start position matches m-1
     * characters and fails on the last one, so naive does about n*m comparisons. There is no match.
     * KMP needs about 2n comparisons here, the most it can ever need.
     */
    public InputPair searchWorstCase(int n, int m) {
        check(n, m);
        return new InputPair("a".repeat(n), "a".repeat(m - 1) + "b");
    }

    /**
     * Many overlapping matches: text a^n and pattern a^m. Every start position matches, so there are
     * n - m + 1 matches. Naive again does about n*m comparisons, KMP about n.
     */
    public InputPair searchAllMatches(int n, int m) {
        check(n, m);
        return new InputPair("a".repeat(n), "a".repeat(m));
    }

    private static void check(int n, int m) {
        if (m < 1 || n < m) {
            throw new IllegalArgumentException("Need 1 <= m <= n, got n=" + n + ", m=" + m);
        }
    }
}
