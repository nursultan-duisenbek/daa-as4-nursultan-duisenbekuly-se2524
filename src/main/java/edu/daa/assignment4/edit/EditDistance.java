package edu.daa.assignment4.edit;

import edu.daa.assignment4.Counters;

/**
 * One interface for all three edit distance versions, so the experiment can run them in one loop.
 */
public interface EditDistance {

    /** Smallest number of insertions, deletions and replacements that turn a into b. */
    int distance(String a, String b);

    /** Counters filled by the most recent call to {@link #distance}. */
    Counters counters();

    /** Short name used in results.csv and in the plots. */
    String name();
}
