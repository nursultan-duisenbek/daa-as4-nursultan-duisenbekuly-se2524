package edu.daa.assignment4.experiment;

import java.util.Arrays;

final class Stats {

    private Stats() {
    }

    /** Median of the values; the mean of the two middle ones when the count is even. */
    static double median(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Cannot take the median of no values.");
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }

    /**
     * Nearest-rank percentile: the smallest value that is at least as large as the given percent of all values.
     * percentile(values, 0) is the minimum and percentile(values, 100) is the maximum.
     */
    static double percentile(double[] values, double percent) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Cannot take a percentile of no values.");
        }
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("The percent must be between 0 and 100, got " + percent);
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int rank = (int) Math.ceil(percent / 100.0 * sorted.length);
        return sorted[Math.max(rank, 1) - 1];
    }
}
