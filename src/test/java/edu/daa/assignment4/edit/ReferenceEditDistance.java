package edu.daa.assignment4.edit;

/**
 * Independent check for the tests: the textbook Wagner-Fischer table with the full three-way minimum
 * and an explicit replacement cost. It does not use the "equal last characters" shortcut that the
 * three real versions share, so a bug in that shortcut cannot hide behind the versions agreeing with each other.
 */
final class ReferenceEditDistance {

    private ReferenceEditDistance() {
    }

    static int distance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[n][m];
    }
}
