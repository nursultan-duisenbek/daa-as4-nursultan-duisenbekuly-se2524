package edu.daa.assignment4.edit;

import edu.daa.assignment4.Counters;

/**
 * Version C: the same table as version B, but only two rows are kept. Time O(n*m), memory O(m).
 *
 * State:      prev[j] = dp[i-1][j] and curr[j] = dp[i][j], the previous and the current row of the table.
 * Transition: same as version B. It reads prev[j-1], prev[j] and curr[j-1]; after each row the two
 *             arrays swap roles, so the old row gets overwritten.
 * Base case:  row 0 is prev[j] = j, and at the start of every row curr[0] = i.
 *             The answer is the last cell of the last row.
 *
 * The price of the saving: the finished rows are gone, so you get the number but cannot trace back
 * which insertions, deletions and replacements produced it.
 */
public final class TwoRowEditDistance implements EditDistance {

    private final Counters counters = new Counters();

    @Override
    public int distance(String a, String b) {
        counters.reset();
        int n = a.length();
        int m = b.length();

        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        counters.memoryCells += prev.length + curr.length;

        for (int j = 0; j <= m; j++) {      // row 0
            prev[j] = j;
            counters.subproblems++;
        }

        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            counters.subproblems++;
            char ai = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                counters.subproblems++;
                if (ai == b.charAt(j - 1)) {
                    curr[j] = prev[j - 1];
                } else {
                    curr[j] = 1 + Math.min(prev[j], Math.min(curr[j - 1], prev[j - 1]));
                }
            }
            int[] swap = prev;              // the finished row becomes the previous row
            prev = curr;
            curr = swap;
        }
        return prev[m];                     // after the last swap, prev holds row n
    }

    @Override
    public Counters counters() {
        return counters;
    }

    @Override
    public String name() {
        return "two_rows";
    }

    @Override
    public String toString() {
        return name();
    }
}
