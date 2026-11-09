package edu.daa.assignment4.experiment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All timed runs of one case (same algorithm, input, n and m) folded into one line: the median time,
 * the fastest and slowest run, and the counters. The counters never change between runs of the same case,
 * and {@link #aggregate} refuses to continue if they do.
 */
public record Summary(String family, String algorithm, String input, int n, int m, int runs,
                      double medianMs, double minMs, double maxMs, long result,
                      long subproblems, long memoryCells, long comparisons, long lpsComparisons) {

    static final String HEADER = "family,algorithm,input,n,m,runs,median_time_ms,min_time_ms,max_time_ms,"
            + "result,subproblems,memory_cells,comparisons,lps_comparisons";

    static List<Summary> aggregate(List<Row> rows) {
        Map<String, List<Row>> cases = new LinkedHashMap<>();
        for (Row row : rows) {
            String key = row.family() + "|" + row.algorithm() + "|" + row.input() + "|" + row.n() + "|" + row.m();
            cases.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        List<Summary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Row>> entry : cases.entrySet()) {
            List<Row> runs = entry.getValue();
            Row first = runs.get(0);
            double[] times = new double[runs.size()];
            for (int i = 0; i < times.length; i++) {
                Row run = runs.get(i);
                if (run.result() != first.result() || run.subproblems() != first.subproblems()
                        || run.memoryCells() != first.memoryCells() || run.comparisons() != first.comparisons()
                        || run.lpsComparisons() != first.lpsComparisons()) {
                    throw new IllegalStateException("Counters or result changed between runs of " + entry.getKey());
                }
                times[i] = run.timeMs();
            }
            double fastest = Double.MAX_VALUE;
            double slowest = 0;
            for (double t : times) {
                fastest = Math.min(fastest, t);
                slowest = Math.max(slowest, t);
            }
            summaries.add(new Summary(first.family(), first.algorithm(), first.input(), first.n(), first.m(),
                    runs.size(), Stats.median(times), fastest, slowest, first.result(),
                    first.subproblems(), first.memoryCells(), first.comparisons(), first.lpsComparisons()));
        }
        return summaries;
    }

    /** Reads one line written by {@link #toCsv()}. Empty counter fields come back as {@link Row#NA}. */
    static Summary fromCsv(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 14) {
            throw new IllegalArgumentException("Expected 14 fields but found " + f.length + ": " + line);
        }
        return new Summary(f[0], f[1], f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]),
                Double.parseDouble(f[6]), Double.parseDouble(f[7]), Double.parseDouble(f[8]), Long.parseLong(f[9]),
                counter(f[10]), counter(f[11]), counter(f[12]), counter(f[13]));
    }

    /** Reads a whole summary.csv, so the plots can be redrawn without measuring again. */
    static List<Summary> readCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
            throw new IOException(file + " is not a summary.csv written by this program (unexpected header).");
        }
        List<Summary> summaries = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (!line.isBlank()) {
                summaries.add(fromCsv(line));
            }
        }
        return summaries;
    }

    private static long counter(String field) {
        return field.isEmpty() ? Row.NA : Long.parseLong(field);
    }

    String toCsv() {
        return String.join(",", family, algorithm, input,
                Integer.toString(n), Integer.toString(m), Integer.toString(runs),
                String.format(Locale.ROOT, "%.6f", medianMs), String.format(Locale.ROOT, "%.6f", minMs),
                String.format(Locale.ROOT, "%.6f", maxMs), Long.toString(result),
                field(subproblems), field(memoryCells), field(comparisons), field(lpsComparisons));
    }

    private static String field(long value) {
        return value == Row.NA ? "" : Long.toString(value);
    }
}
