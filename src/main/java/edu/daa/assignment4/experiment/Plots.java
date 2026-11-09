package edu.daa.assignment4.experiment;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

import edu.daa.assignment4.experiment.LineChart.Marker;
import edu.daa.assignment4.experiment.LineChart.Point;
import edu.daa.assignment4.experiment.LineChart.Series;

/**
 * Builds the four plots from the summary of the measurements. Nothing here is typed in by hand: every point, every
 * label and every number in a caption is read from the summaries, and a caption only states those numbers, so it
 * is true for whatever the data turned out to be.
 *
 * Colour follows the input type in every plot: blue is the random input, orange the worst case, aqua the best case
 * and yellow the all-matches input. A series that is the same for every input is grey.
 */
final class Plots {

    /** A plot that was written: its file name, title and the takeaway sentence printed under it. */
    record Plot(String fileName, String title, String caption) {
    }

    private static final String EDIT = "edit_distance";
    private static final String SEARCH = "string_search";

    /** Two points further apart than this factor on the n axis are not joined by a line. */
    private static final double GAP_FACTOR = 5;

    private Plots() {
    }

    static List<Plot> write(List<Summary> summaries, Config config, Path folder) throws IOException {
        List<Plot> plots = new ArrayList<>();
        plots.add(save(editDistance(summaries, config), "edit_distance.png", folder));
        plots.add(save(twoRowsVsTable(summaries, config), "two_rows_vs_table.png", folder));
        plots.add(save(stringSearch(summaries, config), "string_search.png", folder));
        plots.add(save(performanceComparison(summaries, config), "performance_comparison.png", folder));
        return plots;
    }

    private static Plot save(LineChart chart, String fileName, Path folder) throws IOException {
        chart.write(folder.resolve(fileName));
        return new Plot(fileName, chart.title(), chart.caption());
    }

    // ------------------------------------------------------------------ edit_distance.png

    static LineChart editDistance(List<Summary> s, Config config) {
        int[] sizes = config.smallSizes();
        int last = sizes[sizes.length - 1];

        LineChart chart = new LineChart("Edit distance: subproblems evaluated vs string length",
                "String length n (characters in each of the two strings)",
                "Subproblems evaluated (count, log scale)")
                .linearX(toDoubles(sizes))
                .footnote(countersSource());

        long[] atLast = new long[3];
        String[] inputs = {"random", "disjoint", "identical"};
        String[] describe = {"random strings", "disjoint strings (worst case)", "identical strings (best case)"};
        for (int i = 0; i < inputs.length; i++) {
            List<Point> points = new ArrayList<>();
            for (int n : sizes) {
                points.add(new Point(n, find(s, EDIT, "recursive", inputs[i], n, n).subproblems()));
            }
            atLast[i] = (long) points.get(points.size() - 1).y();
            chart.add(new Series("Recursion, " + describe[i], inputs[i], inputColor(inputs[i]), Marker.CIRCLE, false, points));
        }

        List<Point> tablePoints = new ArrayList<>();
        for (int n : sizes) {
            tablePoints.add(new Point(n, sameForAllInputs(s, "table", n, Summary::subproblems)));
        }
        long tableAtLast = (long) tablePoints.get(tablePoints.size() - 1).y();
        chart.add(new Series("Table, any input", "table", LineChart.NEUTRAL, Marker.DIAMOND, true, tablePoints));

        return chart.caption("At n = " + last + " the plain recursion made " + Fmt.full(atLast[1]) + " calls on disjoint strings, "
                + Fmt.full(atLast[0]) + " on random strings and " + Fmt.full(atLast[2]) + " on identical strings, while the table filled "
                + Fmt.full(tableAtLast) + " cells on every input.");
    }

    // ------------------------------------------------------------------ two_rows_vs_table.png

    static LineChart twoRowsVsTable(List<Summary> s, Config config) {
        int[] sizes = config.editSizes();
        int last = sizes[sizes.length - 1];

        LineChart chart = new LineChart("Edit distance: table compared with two rows",
                "String length n (characters in each of the two strings, log scale)",
                "Table " + Fmt.DIVIDE + " two rows (ratio, log scale)")
                .logX()
                .yTickFormat(Fmt::ratioTick)
                .valueFormat(Fmt::times)
                .footnote(timesSource(s) + " The memory ratio comes from exact counters. Runs with n up to "
                        + config.smallSizes()[config.smallSizes().length - 1] + " take at most "
                        + Fmt.ms(slowestSmallRun(s, config)) + " ms each, so their time ratios are noisy.")
                .reference(1, "1" + Fmt.TIMES + " = same as two rows");

        String[] inputs = {"random", "disjoint", "identical"};
        String[] describe = {"time, random strings", "time, disjoint strings", "time, identical strings"};
        double[] timeAtLast = new double[3];
        for (int i = 0; i < inputs.length; i++) {
            List<Point> points = new ArrayList<>();
            for (int n : sizes) {
                double ratio = find(s, EDIT, "table", inputs[i], n, n).medianMs()
                        / find(s, EDIT, "two_rows", inputs[i], n, n).medianMs();
                addWithGap(points, n, ratio);
            }
            timeAtLast[i] = points.get(points.size() - 1).y();
            chart.add(new Series("Table " + Fmt.DIVIDE + " two rows, " + describe[i], "time, " + inputs[i],
                    inputColor(inputs[i]), Marker.CIRCLE, false, points));
        }

        List<Point> memory = new ArrayList<>();
        for (int n : sizes) {
            double ratio = (double) sameForAllInputs(s, "table", n, Summary::memoryCells)
                    / sameForAllInputs(s, "two_rows", n, Summary::memoryCells);
            addWithGap(memory, n, ratio);
        }
        chart.add(new Series("Table " + Fmt.DIVIDE + " two rows, memory (any input)", "memory", LineChart.NEUTRAL, Marker.DIAMOND, true, memory));

        long tableCells = sameForAllInputs(s, "table", last, Summary::memoryCells);
        long rowCells = sameForAllInputs(s, "two_rows", last, Summary::memoryCells);
        return chart.caption("At n = " + Fmt.full(last) + " the table allocated " + Fmt.full(tableCells) + " ints and two rows "
                + Fmt.full(rowCells) + ", and the table's median time was " + Fmt.times(timeAtLast[0]) + " that of two rows on random strings, "
                + Fmt.times(timeAtLast[1]) + " on disjoint strings and " + Fmt.times(timeAtLast[2]) + " on identical strings.");
    }

    // ------------------------------------------------------------------ string_search.png

    static LineChart stringSearch(List<Summary> s, Config config) {
        int n = config.textLength();
        int[] lengths = config.patternLengths();
        int lastM = lengths[lengths.length - 1];

        LineChart chart = new LineChart("String search: comparisons per text character vs pattern length",
                "Pattern length m (characters, log scale)",
                "Comparisons per text character (count " + Fmt.DIVIDE + " n, log scale)")
                .logX(toDoubles(lengths))
                .xTickFormat(v -> Fmt.tick(v, false))
                .yTickFormat(v -> Fmt.tick(v, false))
                .valueFormat(Fmt::two)
                .footnote(countersSource() + " Text length n = " + Fmt.full(n) + ". Building the LPS array adds at most 2m comparisons.");

        double[][] atLast = new double[2][2];
        String[] inputs = {"random", "worst_case"};
        String[] algorithms = {"naive", "kmp"};
        for (int i = 0; i < inputs.length; i++) {
            for (int a = 0; a < algorithms.length; a++) {
                List<Point> points = new ArrayList<>();
                for (int m : lengths) {
                    points.add(new Point(m, find(s, SEARCH, algorithms[a], inputs[i], n, m).comparisons() / (double) n));
                }
                atLast[i][a] = points.get(points.size() - 1).y();
                String algorithm = a == 0 ? "Naive" : "KMP";
                chart.add(new Series(algorithm + ", " + inputs[i], algorithm + ", " + inputs[i], inputColor(inputs[i]),
                        a == 0 ? Marker.CIRCLE : Marker.SQUARE, a == 1, points));
            }
        }

        return chart.caption("At m = " + lastM + " Naive made " + Fmt.two(atLast[1][0]) + " comparisons per text character on worst_case and KMP made "
                + Fmt.two(atLast[1][1]) + ", while on random text Naive made " + Fmt.two(atLast[0][0]) + " and KMP " + Fmt.two(atLast[0][1]) + ".");
    }

    // ------------------------------------------------------------------ performance_comparison.png

    static LineChart performanceComparison(List<Summary> s, Config config) {
        int m = config.patternLength();
        int[] sizes = config.textSizes();
        int last = sizes[sizes.length - 1];

        LineChart chart = new LineChart("String search: Naive time compared with KMP time",
                "Text length n (characters, log scale)",
                "Naive time " + Fmt.DIVIDE + " KMP time (ratio, log scale)")
                .logX(toDoubles(sizes))
                .xTickFormat(v -> Fmt.tick(v, true))
                .yTickFormat(Fmt::ratioTick)
                .valueFormat(Fmt::times)
                .footnote(timesSource(s) + " Pattern length m = " + m + ". KMP time includes building the LPS array.")
                .reference(1, "1" + Fmt.TIMES + " = same time");

        String[] inputs = {"random", "worst_case", "best_case", "all_matches"};
        double[] atLast = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            List<Point> points = new ArrayList<>();
            for (int n : sizes) {
                points.add(new Point(n, find(s, SEARCH, "naive", inputs[i], n, m).medianMs()
                        / find(s, SEARCH, "kmp", inputs[i], n, m).medianMs()));
            }
            atLast[i] = points.get(points.size() - 1).y();
            chart.add(new Series(inputs[i], inputs[i], inputColor(inputs[i]), Marker.CIRCLE, false, points));
        }

        return chart.caption("At n = " + Fmt.full(last) + " and m = " + m + " Naive took " + Fmt.times(atLast[1]) + " as long as KMP on worst_case, "
                + Fmt.times(atLast[3]) + " on all_matches, " + Fmt.times(atLast[0]) + " on random and " + Fmt.times(atLast[2]) + " on best_case.");
    }

    // ------------------------------------------------------------------ helpers

    private static Color inputColor(String input) {
        switch (input) {
            case "random":
                return LineChart.BLUE;
            case "worst_case":
            case "disjoint":
                return LineChart.ORANGE;
            case "best_case":
            case "identical":
                return LineChart.AQUA;
            case "all_matches":
                return LineChart.YELLOW;
            default:
                throw new IllegalArgumentException("No colour for input " + input);
        }
    }

    private static String countersSource() {
        return "Source: results/summary.csv. Counters are exact and the same on every run.";
    }

    private static String timesSource(List<Summary> s) {
        int runs = s.isEmpty() ? 0 : s.get(0).runs();
        return "Source: results/summary.csv. Times are the median of " + runs + " runs.";
    }

    /** The longest median time of a table or two-row run among the small sizes, which the footnote of plot 2 quotes. */
    private static double slowestSmallRun(List<Summary> s, Config config) {
        int largestSmall = config.smallSizes()[config.smallSizes().length - 1];
        double slowest = 0;
        for (Summary row : s) {
            if (row.family().equals(EDIT) && !row.algorithm().equals("recursive") && row.n() <= largestSmall) {
                slowest = Math.max(slowest, row.medianMs());
            }
        }
        return slowest;
    }

    private static double[] toDoubles(int[] values) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }

    /** Appends a point, after a gap if the previous x is far away, so sizes from two different ranges are not joined. */
    private static void addWithGap(List<Point> points, int n, double y) {
        if (!points.isEmpty() && n / points.get(points.size() - 1).x() > GAP_FACTOR) {
            points.add(Point.gap());
        }
        points.add(new Point(n, y));
    }

    private static Summary find(List<Summary> all, String family, String algorithm, String input, int n, int m) {
        for (Summary row : all) {
            if (row.family().equals(family) && row.algorithm().equals(algorithm) && row.input().equals(input)
                    && row.n() == n && row.m() == m) {
                return row;
            }
        }
        throw new IllegalStateException("No measurement for " + family + " " + algorithm + " " + input + " n=" + n + " m=" + m);
    }

    /**
     * A counter of the table or two-row version that the card predicts to be the same on every input. The plot draws
     * one line for it, so refuse to draw it if the data says otherwise.
     */
    private static long sameForAllInputs(List<Summary> s, String algorithm, int n, ToLongFunction<Summary> counter) {
        long first = counter.applyAsLong(find(s, EDIT, algorithm, "random", n, n));
        for (String input : new String[]{"disjoint", "identical"}) {
            long other = counter.applyAsLong(find(s, EDIT, algorithm, input, n, n));
            if (other != first) {
                throw new IllegalStateException(algorithm + " counter differs between inputs at n=" + n + ": "
                        + first + " vs " + other + " (" + input + ")");
            }
        }
        return first;
    }
}
