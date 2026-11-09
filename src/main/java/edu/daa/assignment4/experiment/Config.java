package edu.daa.assignment4.experiment;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that decides how big the experiment is. {@link #full()} is the experiment of the report, with the sizes
 * written on the Experiment Design Card. {@link #quick()} is a tiny version that only checks that the whole
 * pipeline works; its numbers are never used for results.
 *
 * @param name           "full" or "quick"
 * @param smallSizes     edit distance sizes where all three versions run, including the plain recursion
 * @param largeSizes     further edit distance sizes where only the table and the two-row version run
 * @param textSizes      text lengths of search sweep A (pattern length fixed at patternLength)
 * @param patternLength  pattern length of sweep A
 * @param textLength     text length of search sweep B (pattern length varies)
 * @param patternLengths pattern lengths of sweep B
 * @param runs           timed runs per case; the median is reported
 */
record Config(String name, int[] smallSizes, int[] largeSizes, int[] textSizes, int patternLength,
              int textLength, int[] patternLengths, int runs) {

    /** The plain recursion makes millions of calls from n = 10 on and billions from n = 14, so it is never run above this. */
    static final int MAX_RECURSIVE_SIZE = 14;

    static final int EDIT_INPUT_TYPES = 3;
    static final int SEARCH_INPUT_TYPES = 4;

    Config {
        requireAscending(smallSizes, "smallSizes");
        requireAscending(largeSizes, "largeSizes");
        requireAscending(textSizes, "textSizes");
        requireAscending(patternLengths, "patternLengths");
        if (smallSizes[smallSizes.length - 1] > MAX_RECURSIVE_SIZE) {
            throw new IllegalArgumentException("The recursion must not run above n = " + MAX_RECURSIVE_SIZE
                    + ": it would take hours.");
        }
        if (largeSizes[0] <= smallSizes[smallSizes.length - 1]) {
            throw new IllegalArgumentException("largeSizes must start above smallSizes.");
        }
        if (runs < 3) {
            throw new IllegalArgumentException("Use at least 3 runs per case so the median means something.");
        }
        for (int[] shape : searchShapes(textSizes, patternLength, textLength, patternLengths)) {
            int n = shape[0];
            int m = shape[1];
            if (m < 1 || n / (InputGenerator.PLANTED_COPIES + 1) < m) {
                throw new IllegalArgumentException("Text length " + n + " is too short for pattern length " + m);
            }
        }
    }

    static Config full() {
        return new Config("full",
                new int[]{2, 4, 6, 8, 10, 12},
                new int[]{250, 500, 1000, 2000, 4000},
                new int[]{125_000, 250_000, 500_000, 1_000_000, 2_000_000},
                64,
                1_000_000,
                new int[]{8, 16, 32, 64, 128, 256},
                5);
    }

    static Config quick() {
        return new Config("quick",
                new int[]{2, 4, 6},
                new int[]{50, 100},
                new int[]{6_000, 12_000},
                8,
                12_000,
                new int[]{4, 8, 16},
                3);
    }

    boolean isSmall(int n) {
        for (int size : smallSizes) {
            if (size == n) {
                return true;
            }
        }
        return false;
    }

    /** All edit distance sizes, ascending. */
    int[] editSizes() {
        int[] all = new int[smallSizes.length + largeSizes.length];
        System.arraycopy(smallSizes, 0, all, 0, smallSizes.length);
        System.arraycopy(largeSizes, 0, all, smallSizes.length, largeSizes.length);
        return all;
    }

    /** The (text length, pattern length) pairs of both search sweeps, each pair once. */
    List<int[]> searchShapes() {
        return searchShapes(textSizes, patternLength, textLength, patternLengths);
    }

    /** How many rows results.csv has: one per timed run. */
    int expectedRows() {
        int editCases = smallSizes.length * EDIT_INPUT_TYPES * 3 + largeSizes.length * EDIT_INPUT_TYPES * 2;
        int searchCases = searchShapes().size() * SEARCH_INPUT_TYPES * 2;
        return (editCases + searchCases) * runs;
    }

    private static List<int[]> searchShapes(int[] textSizes, int patternLength, int textLength, int[] patternLengths) {
        List<int[]> shapes = new ArrayList<>();
        for (int n : textSizes) {
            addOnce(shapes, n, patternLength);
        }
        for (int m : patternLengths) {
            addOnce(shapes, textLength, m);
        }
        return shapes;
    }

    private static void addOnce(List<int[]> shapes, int n, int m) {
        for (int[] shape : shapes) {
            if (shape[0] == n && shape[1] == m) {
                return;
            }
        }
        shapes.add(new int[]{n, m});
    }

    private static void requireAscending(int[] values, String what) {
        if (values.length == 0) {
            throw new IllegalArgumentException(what + " must not be empty.");
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 1 || (i > 0 && values[i] <= values[i - 1])) {
                throw new IllegalArgumentException(what + " must be positive and strictly ascending.");
            }
        }
    }
}
