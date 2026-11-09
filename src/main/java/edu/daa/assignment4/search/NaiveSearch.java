package edu.daa.assignment4.search;

import java.util.ArrayList;
import java.util.List;

import edu.daa.assignment4.Counters;

/**
 * Naive search: try every start position and compare character by character.
 *
 * Best case (the first pattern character never matches): one comparison per start position, about n.
 * Worst case (long partial matches that fail late): up to m comparisons per start position, about n*m.
 * After a mismatch it moves one position to the right and compares everything again from the pattern's
 * first character, so it re-reads text characters it has already seen.
 *
 * The counter "comparisons" is increased once for every text-vs-pattern character comparison.
 */
public final class NaiveSearch implements StringSearch {

    private final Counters counters = new Counters();

    @Override
    public List<Integer> search(String text, String pattern) {
        return searchSequence(text, pattern);
    }

    /**
     * Same as {@link #search}, but takes any CharSequence. It is package-private so a test can pass a text
     * that records which positions are read.
     */
    List<Integer> searchSequence(CharSequence text, String pattern) {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("The pattern must not be empty.");
        }
        counters.reset();
        List<Integer> matches = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        for (int start = 0; start + m <= n; start++) {      // every position where the pattern still fits
            int k = 0;
            while (k < m) {
                counters.comparisons++;
                if (text.charAt(start + k) != pattern.charAt(k)) {
                    break;
                }
                k++;
            }
            if (k == m) {
                matches.add(start);
            }
        }
        return matches;
    }

    @Override
    public Counters counters() {
        return counters;
    }

    @Override
    public String name() {
        return "naive";
    }

    @Override
    public String toString() {
        return name();
    }
}
