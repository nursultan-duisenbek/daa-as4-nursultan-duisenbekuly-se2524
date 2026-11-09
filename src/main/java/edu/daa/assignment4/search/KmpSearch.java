package edu.daa.assignment4.search;

import java.util.ArrayList;
import java.util.List;

import edu.daa.assignment4.Counters;

/**
 * Knuth-Morris-Pratt search.
 *
 * Idea: before searching, compute for every prefix of the pattern the length of its longest proper
 * prefix that is also a suffix (the LPS array). When a mismatch happens after j matched characters,
 * the last lps[j-1] of them are still a valid partial match, so the search keeps them and falls back
 * inside the pattern. The text index never goes back, and every text character is read exactly once.
 *
 * Time O(n + m): the pattern index j goes up at most once per text character, and each fall-back
 * lowers it, so there are at most n fall-backs. That gives at most 2n comparisons in the search loop.
 *
 * The counter "comparisons" counts character comparisons in the search loop,
 * "lpsComparisons" counts the ones made while building the LPS array.
 */
public final class KmpSearch implements StringSearch {

    private final Counters counters = new Counters();

    @Override
    public List<Integer> search(String text, String pattern) {
        return searchSequence(text, pattern);
    }

    /**
     * Same as {@link #search}, but takes any CharSequence. It is package-private so a test can pass a text
     * that records which positions are read and check that none of them goes backwards.
     */
    List<Integer> searchSequence(CharSequence text, String pattern) {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("The pattern must not be empty.");
        }
        counters.reset();
        int n = text.length();
        int m = pattern.length();
        int[] lps = lps(pattern);

        List<Integer> matches = new ArrayList<>();
        int j = 0;                                  // number of pattern characters matched so far
        for (int i = 0; i < n; i++) {               // i only ever increases
            char c = text.charAt(i);                // the one and only read of text[i]
            while (true) {
                counters.comparisons++;
                if (pattern.charAt(j) == c) {
                    j++;
                    break;
                }
                if (j == 0) {
                    break;                          // nothing matched yet, move on to the next text character
                }
                j = lps[j - 1];                     // mismatch: fall back inside the pattern, i stays put
            }
            if (j == m) {
                matches.add(i - m + 1);
                j = lps[m - 1];                     // continue from the longest border, so overlaps are found
            }
        }
        return matches;
    }

    /**
     * Builds the LPS array (also called prefix function or failure function).
     * lps[i] is the length of the longest proper prefix of p[0..i] that is also a suffix of p[0..i].
     * "Proper" means shorter than the piece itself.
     * Example: lps("ABABCABAB") = [0, 0, 1, 2, 0, 1, 2, 3, 4].
     */
    public int[] lps(String p) {
        int m = p.length();
        int[] lps = new int[m];
        counters.memoryCells += lps.length;

        int len = 0;                                // length of the border we are trying to extend
        for (int i = 1; i < m; i++) {
            while (true) {
                counters.lpsComparisons++;
                if (p.charAt(i) == p.charAt(len)) {
                    len++;
                    break;
                }
                if (len == 0) {
                    break;
                }
                len = lps[len - 1];                 // try a shorter border
            }
            lps[i] = len;
        }
        return lps;
    }

    @Override
    public Counters counters() {
        return counters;
    }

    @Override
    public String name() {
        return "kmp";
    }

    @Override
    public String toString() {
        return name();
    }
}
