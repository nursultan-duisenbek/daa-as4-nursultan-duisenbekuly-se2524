package edu.daa.assignment4.edit;

import edu.daa.assignment4.Counters;

/**
 * Version A: plain recursion on the last characters, no memory.
 *
 * State:      f(i, j) = edit distance between the first i characters of a and the first j characters of b.
 * Transition: if a[i-1] == b[j-1] then f(i, j) = f(i-1, j-1); otherwise
 *             f(i, j) = 1 + min(f(i-1, j) delete, f(i, j-1) insert, f(i-1, j-1) replace).
 * Base case:  f(0, j) = j (insert j characters) and f(i, 0) = i (delete i characters).
 *
 * It is slow on purpose. Nothing is remembered, so the same pair (i, j) is solved again every time
 * another path reaches it. When no characters match, each call makes three more calls and the number
 * of calls grows exponentially with the length of the strings. The call stack stays shallow, though:
 * every call lowers i + j by at least one, so the depth is at most n + m.
 */
public final class RecursiveEditDistance implements EditDistance {

    private final Counters counters = new Counters();

    @Override
    public int distance(String a, String b) {
        counters.reset();
        return solve(a.toCharArray(), b.toCharArray(), a.length(), b.length());
    }

    private int solve(char[] a, char[] b, int i, int j) {
        counters.subproblems++;             // one more evaluation of f(i, j), repeated ones included
        if (i == 0) {
            return j;
        }
        if (j == 0) {
            return i;
        }
        if (a[i - 1] == b[j - 1]) {
            return solve(a, b, i - 1, j - 1);
        }
        int delete = solve(a, b, i - 1, j);
        int insert = solve(a, b, i, j - 1);
        int replace = solve(a, b, i - 1, j - 1);
        return 1 + Math.min(delete, Math.min(insert, replace));
    }

    @Override
    public Counters counters() {
        return counters;
    }

    @Override
    public String name() {
        return "recursive";
    }

    @Override
    public String toString() {
        return name();
    }
}
