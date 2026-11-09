package edu.daa.assignment4.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Tests that only make sense for KMP: the LPS array and the "never moves backwards" rule. */
class KmpSearchTest {

    @Test
    void lpsReferenceArrays() {
        KmpSearch kmp = new KmpSearch();
        assertArrayEquals(new int[] {0, 1, 2, 0, 1, 2, 3, 3}, kmp.lps("AAACAAAA"));
        assertArrayEquals(new int[] {0, 0, 1, 2, 0, 1, 2, 3, 4}, kmp.lps("ABABCABAB"));
    }

    @Test
    void lpsSmallCases() {
        KmpSearch kmp = new KmpSearch();
        assertArrayEquals(new int[] {}, kmp.lps(""));
        assertArrayEquals(new int[] {0}, kmp.lps("A"));
        assertArrayEquals(new int[] {0, 0, 0, 0}, kmp.lps("ABCD"));
        assertArrayEquals(new int[] {0, 1, 2, 3}, kmp.lps("AAAA"));
        assertArrayEquals(new int[] {0, 0, 1, 2}, kmp.lps("ABAB"));
    }

    @Test
    void lpsMatchesBruteForceOnRandomPatterns() {
        Random random = new Random(2524);
        KmpSearch kmp = new KmpSearch();
        for (int k = 0; k < 100; k++) {
            String pattern = randomString(random, random.nextInt(13), "ab");   // length 0..12
            assertArrayEquals(bruteForceLps(pattern), kmp.lps(pattern), "pattern=\"" + pattern + "\"");
        }
    }

    @Test
    void kmpNeverReadsTheTextBackwards() {
        KmpSearch kmp = new KmpSearch();
        String[][] fixed = {
                {"ABABDABACDABABCABAB", "ABABCABAB"},
                {"AAAAA", "AA"},
                {"ABABABA", "ABA"},
                {"aaaaaaaaaaaaaaaaaaaa", "aaaaab"},     // the pattern keeps falling back
                {"aaaaaaaaaaaaaaaaaaab", "aaaab"},
        };
        for (String[] input : fixed) {
            RecordingText text = new RecordingText(input[0]);
            kmp.searchSequence(text, input[1]);
            assertTrue(text.neverReadsBackwards(), "text=\"" + input[0] + "\", pattern=\"" + input[1] + "\"");
            assertTrue(text.readCount() <= input[0].length(), "each text character is read at most once");
        }

        Random random = new Random(2524);
        for (int k = 0; k < 100; k++) {
            String raw = randomString(random, random.nextInt(61), "ab");
            String pattern = randomString(random, 1 + random.nextInt(6), "ab");
            RecordingText text = new RecordingText(raw);
            kmp.searchSequence(text, pattern);
            assertTrue(text.neverReadsBackwards(), "text=\"" + raw + "\", pattern=\"" + pattern + "\"");
        }
    }

    /** Makes sure the check above can fail: the naive search does go back after a failed partial match. */
    @Test
    void naiveSearchDoesReadTheTextBackwards() {
        RecordingText text = new RecordingText("AAAB");
        new NaiveSearch().searchSequence(text, "AAB");
        assertFalse(text.neverReadsBackwards());
    }

    @Test
    void kmpUsesAtMostTwoComparisonsPerTextCharacter() {
        KmpSearch kmp = new KmpSearch();
        String[][] inputs = {
                {"a".repeat(1000), "a".repeat(49) + "b"},      // close to the worst case for KMP
                {"a".repeat(1000), "a".repeat(50)},
                {"ab".repeat(500), "abababab"},
                {"abcdefghij".repeat(100), "jab"},
        };
        for (String[] input : inputs) {
            kmp.search(input[0], input[1]);
            assertTrue(kmp.counters().comparisons <= 2L * input[0].length(),
                    "comparisons=" + kmp.counters().comparisons + " for n=" + input[0].length());
            assertTrue(kmp.counters().lpsComparisons <= 2L * input[1].length());
        }
    }

    @Test
    void searchAgreesWithTheReferenceArrayExample() {
        // Searching with the pattern whose LPS array is in the assignment.
        KmpSearch kmp = new KmpSearch();
        assertEquals(List.of(10), kmp.search("ABABDABACDABABCABAB", "ABABCABAB"));
    }

    /** Longest proper border of every prefix, found the slow obvious way. */
    private static int[] bruteForceLps(String p) {
        int[] result = new int[p.length()];
        for (int i = 0; i < p.length(); i++) {
            String prefix = p.substring(0, i + 1);
            for (int len = i; len >= 1; len--) {
                if (prefix.regionMatches(0, prefix, prefix.length() - len, len)) {
                    result[i] = len;
                    break;
                }
            }
        }
        return result;
    }

    private static String randomString(Random random, int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
