package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the quick version of the experiment from start to finish. It checks that the pipeline works and that its
 * output is consistent; it says nothing about the real results, which come from the full configuration.
 */
class ExperimentSmokeTest {

    private static final PrintStream QUIET = new PrintStream(OutputStream.nullOutputStream());

    @TempDir
    Path folder;

    private static List<String[]> readCsv(Path file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            rows.add(line.split(",", -1));
        }
        return rows;
    }

    // ------------------------------------------------------------------ measurements (no drawing needed)

    @Test
    void measurementsCoverEveryCaseAndTheCountersFollowTheFormulas() {
        Config config = Config.quick();
        List<Row> rows = Experiment.measure(config, QUIET);
        assertEquals(config.expectedRows(), rows.size());

        for (Row row : rows) {
            assertTrue(row.timeMs() > 0, "time must be positive: " + row);
            if (!row.family().equals("edit_distance")) {
                continue;
            }
            long n = row.n();
            switch (row.algorithm()) {
                case "table":
                    assertEquals((n + 1) * (n + 1), row.subproblems(), row.toString());
                    assertEquals((n + 1) * (n + 1), row.memoryCells(), row.toString());
                    break;
                case "two_rows":
                    assertEquals((n + 1) * (n + 1), row.subproblems(), row.toString());
                    assertEquals(2 * (n + 1), row.memoryCells(), row.toString());
                    break;
                case "recursive":
                    if (row.input().equals("identical")) {
                        assertEquals(n + 1, row.subproblems(), row.toString());
                        assertEquals(0, row.result());
                    }
                    if (row.input().equals("disjoint")) {
                        assertEquals(n, row.result(), "no character matches, so every position is a replacement");
                    }
                    break;
                default:
                    throw new AssertionError("unexpected algorithm " + row.algorithm());
            }
        }
    }

    @Test
    void theRecursionOnlyRunsOnTheSmallSizes() {
        Config config = Config.quick();
        int largestSmall = config.smallSizes()[config.smallSizes().length - 1];
        for (Row row : Experiment.measure(config, QUIET)) {
            if (row.algorithm().equals("recursive")) {
                assertTrue(row.n() <= largestSmall, "recursion ran at n = " + row.n());
            }
        }
    }

    @Test
    void sameSeedGivesSameInputsAndSameCountersOnEveryRun() {
        List<Row> first = Experiment.measure(Config.quick(), QUIET);
        List<Row> second = Experiment.measure(Config.quick(), QUIET);
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Row a = first.get(i);
            Row b = second.get(i);
            // Everything except the time is exactly reproducible.
            assertEquals(a.toCsv().replaceFirst("^(([^,]*,){6})[^,]*", "$1"), b.toCsv().replaceFirst("^(([^,]*,){6})[^,]*", "$1"),
                    "row " + i + " differs between two runs");
        }
    }

    @Test
    void everyCaseGetsItsOwnSeedAndTheSeedNeverChanges() {
        assertEquals(Experiment.seedFor("edit|random|12"), Experiment.seedFor("edit|random|12"));
        assertFalse(Experiment.seedFor("edit|random|12") == Experiment.seedFor("edit|random|10"));
        assertFalse(Experiment.seedFor("edit|random|12") == Experiment.seedFor("edit|disjoint|12"));
        // Values pinned on purpose (computed independently in Python with the same SplitMix64 steps): if the
        // derivation ever changes, every input of the experiment would change with it.
        assertEquals(-2216049090728454539L, Experiment.seedFor("edit|random|12"));
        assertEquals(5530433697265862268L, Experiment.seedFor("search|worst_case|1000000|64"));
    }

    // ------------------------------------------------------------------ whole pipeline

    private static boolean canDrawText() {
        return LineChartTest.canDrawText();
    }

    private static int countNear(BufferedImage image, Color color) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y));
                if (Math.abs(c.getRed() - color.getRed()) <= 6 && Math.abs(c.getGreen() - color.getGreen()) <= 6
                        && Math.abs(c.getBlue() - color.getBlue()) <= 6) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void quickRunWritesAllFilesAndTheDrawnPlotsContainTheData() throws IOException {
        Assumptions.assumeTrue(canDrawText(), "this Java installation cannot draw text (no fonts), so plots cannot be written");
        Config config = Config.quick();
        List<Plots.Plot> plots = Experiment.run(config, folder, QUIET);

        // results.csv: header, one line per timed run, 12 fields each, times positive.
        List<String[]> results = readCsv(folder.resolve("results.csv"));
        assertEquals(Row.HEADER, String.join(",", results.get(0)));
        assertEquals(config.expectedRows() + 1, results.size());
        for (String[] fields : results) {
            assertEquals(12, fields.length);
        }
        for (String[] fields : results.subList(1, results.size())) {
            assertTrue(Double.parseDouble(fields[6]) > 0, "time_ms > 0");
        }

        // summary.csv: one line per case.
        List<String[]> summary = readCsv(folder.resolve("summary.csv"));
        assertEquals(Summary.HEADER, String.join(",", summary.get(0)));
        assertEquals(config.expectedRows() / config.runs() + 1, summary.size());

        // environment.txt says where the numbers come from.
        String environment = Files.readString(folder.resolve("environment.txt"));
        assertTrue(environment.contains("seed=42"));
        assertTrue(environment.contains("configuration=quick"));
        assertTrue(environment.contains("java.version="));

        // Four plots, each a real picture of the right size that uses the colours of its series.
        assertEquals(List.of("edit_distance.png", "two_rows_vs_table.png", "string_search.png", "performance_comparison.png"),
                plots.stream().map(Plots.Plot::fileName).collect(Collectors.toList()));
        for (Plots.Plot plot : plots) {
            BufferedImage image = ImageIO.read(folder.resolve("plots").resolve(plot.fileName()).toFile());
            assertEquals(LineChart.IMAGE_WIDTH, image.getWidth(), plot.fileName());
            assertTrue(countNear(image, LineChart.BLUE) > 300, plot.fileName() + " has blue data");
            assertTrue(countNear(image, LineChart.ORANGE) > 300, plot.fileName() + " has orange data");
        }
        assertTrue(countNear(ImageIO.read(folder.resolve("plots/edit_distance.png").toFile()), LineChart.AQUA) > 300);
        assertTrue(countNear(ImageIO.read(folder.resolve("plots/performance_comparison.png").toFile()), LineChart.YELLOW) > 300);

        // Captions are one sentence each and state numbers that come from the counters.
        String captions = Files.readString(folder.resolve("plots/captions.txt"));
        for (Plots.Plot plot : plots) {
            assertTrue(plot.caption().endsWith("."), plot.caption());
            assertFalse(plot.caption().contains(". "), "one sentence only: " + plot.caption());
            assertTrue(captions.contains(plot.caption()));
        }
        Plots.Plot editPlot = plots.get(0);
        assertTrue(editPlot.caption().contains("13,483 calls on disjoint strings"), editPlot.caption()); // (3*8989-1)/2 at n = 6
        assertTrue(editPlot.caption().contains("filled 49 cells on every input"), editPlot.caption());
    }

    @Test
    void plotsCanBeRedrawnFromTheSummaryFileWithoutMeasuringAgain() throws IOException {
        Assumptions.assumeTrue(canDrawText(), "this Java installation cannot draw text (no fonts), so plots cannot be written");
        Config config = Config.quick();
        List<Plots.Plot> first = Experiment.run(config, folder, QUIET);
        byte[] results = Files.readAllBytes(folder.resolve("results.csv"));
        byte[] summary = Files.readAllBytes(folder.resolve("summary.csv"));
        for (Plots.Plot plot : first) {
            Files.delete(folder.resolve("plots").resolve(plot.fileName()));
        }

        List<Plots.Plot> again = Experiment.redraw(config, folder, QUIET);

        assertEquals(first.size(), again.size());
        for (Plots.Plot plot : again) {
            assertTrue(Files.size(folder.resolve("plots").resolve(plot.fileName())) > 10_000, plot.fileName() + " was drawn again");
        }
        // The two plots drawn from counters read exactly the same numbers back; the two drawn from times are
        // computed from times rounded to a nanosecond in the CSV, so only their shape is compared.
        assertEquals(first.get(0).caption(), again.get(0).caption());
        assertEquals(first.get(2).caption(), again.get(2).caption());
        assertTrue(again.get(1).caption().endsWith(".") && again.get(3).caption().endsWith("."));
        // Redrawing must not touch the measurements.
        assertEquals(java.util.Arrays.hashCode(results), java.util.Arrays.hashCode(Files.readAllBytes(folder.resolve("results.csv"))));
        assertEquals(java.util.Arrays.hashCode(summary), java.util.Arrays.hashCode(Files.readAllBytes(folder.resolve("summary.csv"))));
    }

    @Test
    void redrawingWithoutASummaryFileFailsClearly() {
        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> Experiment.redraw(Config.quick(), folder, QUIET));
    }
}
