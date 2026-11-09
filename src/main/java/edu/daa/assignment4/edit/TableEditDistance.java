package edu.daa.assignment4.edit;

import edu.daa.assignment4.Counters;

/**
 * Version B: bottom-up table with (n+1) x (m+1) cells. Time O(n*m), memory O(n*m).
 *
 * State:      dp[i][j] = edit distance between the first i characters of a and the first j characters of b.
 * Transition: dp[i][j] = dp[i-1][j-1] if a[i-1] == b[j-1], otherwise
 *             1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]). Rows are filled top to bottom,
 *             so the three cells on the right-hand side are always ready.
 * Base case:  dp[i][0] = i and dp[0][j] = j. The answer is dp[n][m].
 *
 * Every pair (i, j) is solved exactly once, which is what removes the repeated work of version A.
 */
public final class TableEditDistance implements EditDistance {

    private final Counters counters = new Counters();

    @Override
    public int distance(String a, String b) {
        counters.reset();
        int n = a.length();
        int m = b.length();

        // Allocate row by row so the memory counter adds up the lengths of the arrays we really created.
        int[][] dp = new int[n + 1][];
        for (int i = 0; i <= n; i++) {
            dp[i] = new int[m + 1];
            counters.memoryCells += dp[i].length;
        }

        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
            counters.subproblems++;
        }
        for (int j = 1; j <= m; j++) {
            dp[0][j] = j;
            counters.subproblems++;
        }

        for (int i = 1; i <= n; i++) {
            char ai = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                counters.subproblems++;
                if (ai == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        return dp[n][m];
    }

    @Override
    public Counters counters() {
        return counters;
    }

    @Override
    public String name() {
        return "table";
    }

    @Override
    public String toString() {
        return name();
    }
}
