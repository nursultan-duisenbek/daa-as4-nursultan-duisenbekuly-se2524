package edu.daa.assignment4.search;

import java.util.List;

import edu.daa.assignment4.Counters;

/**
 * One interface for both string search methods, so the experiment can run them in one loop.
 */
public interface StringSearch {

    /**
     * Start indices (counting from 0) of all occurrences of the pattern in the text, in increasing
     * order, overlapping occurrences included. The list is empty when there is no match, and also
     * when the pattern is longer than the text.
     *
     * @throws IllegalArgumentException if the pattern is empty
     */
    List<Integer> search(String text, String pattern);

    /** Counters filled by the most recent call to {@link #search}. */
    Counters counters();

    /** Short name used in results.csv and in the plots. */
    String name();
}
