package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StatsTest {

    @Test
    void medianOfAnOddCountIsTheMiddleValue() {
        assertEquals(3.0, Stats.median(new double[]{5, 1, 3}), 0.0);
    }

    @Test
    void medianOfAnEvenCountIsTheMeanOfTheTwoMiddleValues() {
        assertEquals(2.5, Stats.median(new double[]{4, 1, 3, 2}), 0.0);
    }

    @Test
    void medianOfOneValueIsThatValue() {
        assertEquals(7.5, Stats.median(new double[]{7.5}), 0.0);
    }

    @Test
    void medianIgnoresOneVerySlowRun() {
        // This is why the experiment reports the median of its runs and not the mean: one run hit by a GC pause
        // or a busy machine does not move it.
        assertEquals(1.0, Stats.median(new double[]{1, 1, 1000, 1, 1}), 0.0);
    }

    @Test
    void medianDoesNotReorderTheCallersArray() {
        double[] values = {3, 1, 2};
        Stats.median(values);
        assertArrayEquals(new double[]{3, 1, 2}, values, 0.0);
    }

    @Test
    void medianOfNothingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Stats.median(new double[0]));
    }

    @Test
    void percentileUsesTheNearestRank() {
        double[] oneToTen = {7, 3, 10, 1, 5, 9, 2, 8, 4, 6};
        assertEquals(1.0, Stats.percentile(oneToTen, 0), 0.0, "0% is the minimum");
        assertEquals(1.0, Stats.percentile(oneToTen, 5), 0.0, "rank ceil(0.5) = 1");
        assertEquals(5.0, Stats.percentile(oneToTen, 50), 0.0, "rank 5");
        assertEquals(9.0, Stats.percentile(oneToTen, 90), 0.0, "rank 9");
        assertEquals(10.0, Stats.percentile(oneToTen, 95), 0.0, "rank ceil(9.5) = 10");
        assertEquals(10.0, Stats.percentile(oneToTen, 100), 0.0, "100% is the maximum");
    }

    @Test
    void percentileDoesNotReorderTheCallersArray() {
        double[] values = {3, 1, 2};
        Stats.percentile(values, 50);
        assertArrayEquals(new double[]{3, 1, 2}, values, 0.0);
    }

    @Test
    void percentileOfNothingOrOutsideZeroToHundredIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Stats.percentile(new double[0], 50));
        assertThrows(IllegalArgumentException.class, () -> Stats.percentile(new double[]{1}, -1));
        assertThrows(IllegalArgumentException.class, () -> Stats.percentile(new double[]{1}, 100.5));
    }
}
