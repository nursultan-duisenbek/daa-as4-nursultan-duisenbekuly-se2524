package edu.daa.assignment4;

/**
 * Operation counters. Each algorithm owns one instance and increments the fields while it runs,
 * so every number is something the code actually did, not a formula applied afterwards.
 * Every call to distance() or search() starts again from zero.
 *
 * What the fields measure:
 *
 *   subproblems     Edit distance only. How many times the recurrence was evaluated for a pair (i, j).
 *                   Recursive version: every call, repeated pairs included.
 *                   Table and two-row versions: every cell that gets filled, base cells included.
 *
 *   memoryCells     Number of int cells in the arrays the algorithm allocated for itself
 *                   (DP table, DP rows, LPS array). The input strings and the result list are not counted.
 *
 *   comparisons     String search only. Character comparisons between text and pattern in the search loop.
 *
 *   lpsComparisons  KMP only. Character comparisons made while building the LPS array.
 *
 * The fields are plain public longs on purpose: an increment costs almost nothing inside the hot loops.
 */
public final class Counters {
    public long subproblems;
    public long memoryCells;
    public long comparisons;
    public long lpsComparisons;

    public void reset() {
        subproblems = 0;
        memoryCells = 0;
        comparisons = 0;
        lpsComparisons = 0;
    }

    @Override
    public String toString() {
        return "subproblems=" + subproblems + ", memoryCells=" + memoryCells
                + ", comparisons=" + comparisons + ", lpsComparisons=" + lpsComparisons;
    }
}
