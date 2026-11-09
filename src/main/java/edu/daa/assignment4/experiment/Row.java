package edu.daa.assignment4.experiment;

import java.util.Locale;

import edu.daa.assignment4.Counters;

/**
 * One timed run, which is one row of results.csv.
 *
 * "result" is the edit distance for edit distance rows and the number of matches for string search rows.
 * A counter that does not apply to the family is NA and is written as an empty field.
 */
record Row(String family, String algorithm, String input, int n, int m, int run, double timeMs, long result,
           long subproblems, long memoryCells, long comparisons, long lpsComparisons) {

    static final long NA = -1;

    static final String HEADER =
            "family,algorithm,input,n,m,run,time_ms,result,subproblems,memory_cells,comparisons,lps_comparisons";

    /** Edit distance rows have n = |a| and m = |b|; the experiment always uses the same length for both. */
    static Row edit(String algorithm, Counters c, String input, int n, int run, double timeMs, long distance) {
        return new Row("edit_distance", algorithm, input, n, n, run, timeMs, distance,
                c.subproblems, c.memoryCells, NA, NA);
    }

    /** String search rows have n = text length and m = pattern length. */
    static Row search(String algorithm, Counters c, String input, int n, int m, int run, double timeMs, long matches) {
        return new Row("string_search", algorithm, input, n, m, run, timeMs, matches,
                NA, c.memoryCells, c.comparisons, c.lpsComparisons);
    }

    String toCsv() {
        // Locale.ROOT keeps the decimal point a dot on every machine, so the file always parses.
        return String.join(",", family, algorithm, input,
                Integer.toString(n), Integer.toString(m), Integer.toString(run),
                String.format(Locale.ROOT, "%.6f", timeMs), Long.toString(result),
                field(subproblems), field(memoryCells), field(comparisons), field(lpsComparisons));
    }

    private static String field(long value) {
        return value == NA ? "" : Long.toString(value);
    }
}
