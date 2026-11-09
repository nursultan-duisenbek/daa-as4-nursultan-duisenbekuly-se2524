package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RandomSpreadTest {

    private static final PrintStream QUIET = new PrintStream(OutputStream.nullOutputStream());

    @TempDir
    Path folder;

    @Test
    void writesOneRowPerPairWithTheDocumentedColumns() throws IOException {
        RandomSpread.Result result = RandomSpread.run(6, 20, folder, QUIET);

        List<String> lines = Files.readAllLines(folder.resolve("random_spread.csv"));
        assertEquals(RandomSpread.HEADER, lines.get(0));
        assertEquals(21, lines.size(), "header plus one row per pair");
        String[] first = lines.get(1).split(",", -1);
        assertEquals(5, first.length);
        assertEquals("6", first[0]);
        assertEquals("1", first[1]);
        assertEquals(Long.toString(result.counts()[0]), first[2]);
        assertEquals(Long.toString(result.disjoint()), first[3]);
        assertEquals(String.format(java.util.Locale.ROOT, "%.4f", result.counts()[0] / (double) result.disjoint()), first[4]);
    }

    @Test
    void theDisjointReferenceHasTheCallCountOfTheDelannoyFormula() throws IOException {
        // D(6,6) = 8989, so (3 * 8989 - 1) / 2 = 13,483 calls. The class counts it by running the recursion.
        assertEquals(13_483, RandomSpread.run(6, 1, folder, QUIET).disjoint());
    }

    @Test
    void theMainPairIsTheRandomPairOfTheMainExperiment() throws IOException {
        // results.csv of the full run records 227,738 calls for the random pair at n = 8 and 398,593 for the disjoint one.
        RandomSpread.Result result = RandomSpread.run(8, 1, folder, QUIET);
        assertEquals(227_738, result.mainPair());
        assertEquals(398_593, result.disjoint());
    }

    @Test
    void everyRandomPairNeedsBetweenNPlusOneCallsAndTheDisjointCount() throws IOException {
        // n + 1 calls is the best case (a single diagonal path); no pair can need more than the pair without any match.
        RandomSpread.Result result = RandomSpread.run(6, 50, folder, QUIET);
        for (long calls : result.counts()) {
            assertTrue(calls >= 7 && calls <= result.disjoint(), "calls " + calls);
        }
        assertTrue(result.mainPair() >= 7 && result.mainPair() <= result.disjoint());
    }

    @Test
    void theCountsDifferFromPairToPair() throws IOException {
        // This is the reason for the follow-up: one random pair says little about the typical count.
        RandomSpread.Result result = RandomSpread.run(8, 30, folder, QUIET);
        long min = Long.MAX_VALUE;
        long max = 0;
        for (long calls : result.counts()) {
            min = Math.min(min, calls);
            max = Math.max(max, calls);
        }
        assertTrue(max > 1.5 * min, "min " + min + ", max " + max);
    }

    @Test
    void theSameSeedGivesTheSameCountsAndTheSameFile() throws IOException {
        Path other = Files.createDirectory(folder.resolve("again"));
        RandomSpread.Result first = RandomSpread.run(7, 15, folder, QUIET);
        RandomSpread.Result second = RandomSpread.run(7, 15, other, QUIET);
        assertArrayEquals(first.counts(), second.counts());
        assertEquals(Files.readString(folder.resolve("random_spread.csv")), Files.readString(other.resolve("random_spread.csv")));
    }

    @Test
    void theSummaryNamesTheMainPairAndTheSpread() throws IOException {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        RandomSpread.run(6, 10, folder, new PrintStream(captured, true, StandardCharsets.UTF_8));
        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("random pair of the main experiment"), text);
        assertTrue(text.contains("median"), text);
        assertTrue(text.contains("95th percentile"), text);
        assertTrue(text.contains("of 10"), text);
    }

    @Test
    void aSizeTheRecursionCannotSurviveOrNoPairsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RandomSpread.run(Config.MAX_RECURSIVE_SIZE + 1, 1, folder, QUIET));
        assertThrows(IllegalArgumentException.class, () -> RandomSpread.run(0, 1, folder, QUIET));
        assertThrows(IllegalArgumentException.class, () -> RandomSpread.run(6, 0, folder, QUIET));
    }
}
