package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Pins the sizes of the real experiment to the assignment's rules, so an edit to Config cannot silently break them. */
class ConfigTest {

    private static void assertAtLeastFourSizesAndFourTimesRange(int[] sizes, String what) {
        assertTrue(sizes.length >= 4, what + " needs at least 4 sizes");
        assertTrue(sizes[sizes.length - 1] >= 4 * sizes[0], what + ": largest size must be at least 4x the smallest");
    }

    @Test
    void fullConfigurationMeetsTheSizeRulesOfTheAssignment() {
        Config full = Config.full();
        assertAtLeastFourSizesAndFourTimesRange(full.smallSizes(), "recursion");
        assertAtLeastFourSizesAndFourTimesRange(full.editSizes(), "table and two rows");
        assertAtLeastFourSizesAndFourTimesRange(full.textSizes(), "search, sweep over the text length");
        assertAtLeastFourSizesAndFourTimesRange(full.patternLengths(), "search, sweep over the pattern length");
        assertTrue(full.runs() >= 3 && full.runs() <= 5, "the assignment asks for the median of 3 to 5 runs");
    }

    @Test
    void fullConfigurationHasTheSizesOfTheDesignCard() {
        Config full = Config.full();
        assertEquals(List.of(2, 4, 6, 8, 10, 12), boxed(full.smallSizes()));
        assertEquals(List.of(250, 500, 1000, 2000, 4000), boxed(full.largeSizes()));
        assertEquals(List.of(125_000, 250_000, 500_000, 1_000_000, 2_000_000), boxed(full.textSizes()));
        assertEquals(64, full.patternLength());
        assertEquals(1_000_000, full.textLength());
        assertEquals(List.of(8, 16, 32, 64, 128, 256), boxed(full.patternLengths()));
        assertEquals(5, full.runs());
    }

    @Test
    void theRecursionNeverRunsAboveTheSafetyLimit() {
        assertTrue(Config.full().smallSizes()[Config.full().smallSizes().length - 1] <= Config.MAX_RECURSIVE_SIZE);
        assertThrows(IllegalArgumentException.class, () -> new Config("x", new int[]{2, 15}, new int[]{100},
                new int[]{6_000}, 8, 12_000, new int[]{4}, 3));
    }

    @Test
    void expectedRowCountsAddUpToOneRowPerTimedRun() {
        // Edit distance: 6 small sizes x 3 inputs x 3 versions + 5 large sizes x 3 inputs x 2 versions = 84 cases.
        // Search: 10 distinct (n, m) shapes x 4 inputs x 2 algorithms = 80 cases. 164 cases x 5 runs = 820 rows.
        assertEquals(820, Config.full().expectedRows());
        // Quick: (3 x 3 x 3 + 2 x 3 x 2) + 4 shapes x 4 x 2 = 71 cases x 3 runs.
        assertEquals(213, Config.quick().expectedRows());
    }

    @Test
    void sharedSearchShapeIsListedOnce() {
        long shared = Config.full().searchShapes().stream()
                .filter(shape -> shape[0] == 1_000_000 && shape[1] == 64).count();
        assertEquals(1, shared);
        assertEquals(10, Config.full().searchShapes().size());
    }

    @Test
    void invalidConfigurationsAreRejected() {
        int[] small = {2, 4};
        int[] large = {100};
        int[] texts = {6_000, 12_000};
        int[] patterns = {4, 8};
        assertThrows(IllegalArgumentException.class, () -> new Config("x", small, large, texts, 8, 12_000, patterns, 2),
                "fewer than 3 runs");
        assertThrows(IllegalArgumentException.class, () -> new Config("x", new int[]{4, 2}, large, texts, 8, 12_000, patterns, 3),
                "sizes not ascending");
        assertThrows(IllegalArgumentException.class, () -> new Config("x", small, new int[]{3}, texts, 8, 12_000, patterns, 3),
                "large sizes must start above the small ones");
        assertThrows(IllegalArgumentException.class, () -> new Config("x", small, large, texts, 5_000, 12_000, patterns, 3),
                "text too short to plant five copies of the pattern");
        assertThrows(IllegalArgumentException.class, () -> new Config("x", small, large, new int[0], 8, 12_000, patterns, 3),
                "empty sizes");
    }

    private static List<Integer> boxed(int[] values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
