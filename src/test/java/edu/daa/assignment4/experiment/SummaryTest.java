package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.daa.assignment4.Counters;

class SummaryTest {

    @TempDir
    Path folder;

    private static Counters counters(long subproblems, long memoryCells, long comparisons, long lpsComparisons) {
        Counters c = new Counters();
        c.subproblems = subproblems;
        c.memoryCells = memoryCells;
        c.comparisons = comparisons;
        c.lpsComparisons = lpsComparisons;
        return c;
    }

    private static Row editRow(String algorithm, int n, int run, double timeMs) {
        return Row.edit(algorithm, counters(169, 169, 0, 0), "random", n, run, timeMs, 9);
    }

    @Test
    void editRowLeavesTheSearchCountersEmpty() {
        Row row = Row.edit("table", counters(169, 169, 0, 0), "random", 12, 1, 0.001234, 9);
        assertEquals("edit_distance,table,random,12,12,1,0.001234,9,169,169,,", row.toCsv());
    }

    @Test
    void searchRowLeavesTheSubproblemCounterEmpty() {
        Row row = Row.search("kmp", counters(0, 64, 1_000_000, 126), "worst_case", 1_000_000, 64, 3, 2.5, 0);
        assertEquals("string_search,kmp,worst_case,1000000,64,3,2.500000,0,,64,1000000,126", row.toCsv());
    }

    @Test
    void everyCsvLineHasAsManyFieldsAsItsHeader() {
        Row edit = editRow("table", 12, 1, 0.5);
        Row search = Row.search("naive", counters(0, 0, 10, 0), "random", 100, 4, 1, 0.5, 2);
        assertEquals(12, Row.HEADER.split(",").length);
        assertEquals(12, edit.toCsv().split(",", -1).length);
        assertEquals(12, search.toCsv().split(",", -1).length);
    }

    @Test
    void csvUsesADecimalPointWhateverTheDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String timeField = editRow("table", 12, 1, 1.5).toCsv().split(",")[6];
            assertEquals("1.500000", timeField);
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    void aggregateTakesTheMedianMinimumAndMaximumOfTheRuns() {
        List<Row> rows = new ArrayList<>();
        double[] times = {5, 1, 3, 2, 4};
        for (int run = 1; run <= 5; run++) {
            rows.add(editRow("table", 12, run, times[run - 1]));
        }
        List<Summary> summaries = Summary.aggregate(rows);
        assertEquals(1, summaries.size());
        Summary s = summaries.get(0);
        assertEquals(5, s.runs());
        assertEquals(3.0, s.medianMs(), 0.0);
        assertEquals(1.0, s.minMs(), 0.0);
        assertEquals(5.0, s.maxMs(), 0.0);
        assertEquals(169, s.subproblems());
        assertEquals(169, s.memoryCells());
        assertEquals(9, s.result());
    }

    @Test
    void aggregateKeepsDifferentCasesApartInTheOrderTheyWereRun() {
        List<Row> rows = new ArrayList<>();
        for (int run = 1; run <= 3; run++) {
            rows.add(editRow("table", 12, run, 1));
        }
        for (int run = 1; run <= 3; run++) {
            rows.add(Row.edit("two_rows", counters(169, 26, 0, 0), "random", 12, run, 1, 9));
        }
        for (int run = 1; run <= 3; run++) {
            rows.add(editRow("table", 20, run, 1));
        }
        List<Summary> summaries = Summary.aggregate(rows);
        assertEquals(3, summaries.size());
        assertEquals("table", summaries.get(0).algorithm());
        assertEquals(12, summaries.get(0).n());
        assertEquals("two_rows", summaries.get(1).algorithm());
        assertEquals(26, summaries.get(1).memoryCells());
        assertEquals(20, summaries.get(2).n());
    }

    @Test
    void aggregateRefusesRunsWhoseCountersDiffer() {
        List<Row> rows = List.of(
                Row.edit("table", counters(169, 169, 0, 0), "random", 12, 1, 1.0, 9),
                Row.edit("table", counters(170, 169, 0, 0), "random", 12, 2, 1.0, 9));
        assertThrows(IllegalStateException.class, () -> Summary.aggregate(rows));
    }

    @Test
    void aggregateRefusesRunsWhoseResultDiffers() {
        List<Row> rows = List.of(
                Row.edit("table", counters(169, 169, 0, 0), "random", 12, 1, 1.0, 9),
                Row.edit("table", counters(169, 169, 0, 0), "random", 12, 2, 1.0, 8));
        assertThrows(IllegalStateException.class, () -> Summary.aggregate(rows));
    }

    @Test
    void summaryCsvHasAsManyFieldsAsItsHeader() {
        List<Row> rows = new ArrayList<>();
        for (int run = 1; run <= 3; run++) {
            rows.add(editRow("table", 12, run, run));
        }
        String line = Summary.aggregate(rows).get(0).toCsv();
        assertEquals(14, Summary.HEADER.split(",").length);
        assertEquals(14, line.split(",", -1).length);
        assertTrue(line.startsWith("edit_distance,table,random,12,12,3,2.000000,1.000000,3.000000,9,169,169,,"), line);
    }

    @Test
    void csvLineReadsBackToTheSameSummary() {
        List<Row> rows = new ArrayList<>();
        double[] times = {0.5, 0.25, 1.125};
        for (int run = 1; run <= 3; run++) {
            rows.add(Row.search("kmp", counters(0, 64, 1_000_000, 126), "worst_case", 1_000_000, 64, run, times[run - 1], 0));
        }
        Summary original = Summary.aggregate(rows).get(0);
        Summary read = Summary.fromCsv(original.toCsv());
        assertEquals(original.family(), read.family());
        assertEquals(original.algorithm(), read.algorithm());
        assertEquals(original.input(), read.input());
        assertEquals(original.n(), read.n());
        assertEquals(original.m(), read.m());
        assertEquals(original.runs(), read.runs());
        assertEquals(original.medianMs(), read.medianMs(), 1e-6);
        assertEquals(original.minMs(), read.minMs(), 1e-6);
        assertEquals(original.maxMs(), read.maxMs(), 1e-6);
        assertEquals(original.result(), read.result());
        assertEquals(Row.NA, read.subproblems(), "an empty counter field is read back as not applicable");
        assertEquals(64, read.memoryCells());
        assertEquals(1_000_000, read.comparisons());
        assertEquals(126, read.lpsComparisons());
    }

    @Test
    void fromCsvRejectsALineWithTheWrongNumberOfFields() {
        assertThrows(IllegalArgumentException.class, () -> Summary.fromCsv("edit_distance,table,random,12"));
    }

    @Test
    void summaryFileIsReadBackLineForLine() throws IOException {
        List<Row> rows = new ArrayList<>();
        for (int run = 1; run <= 3; run++) {
            rows.add(editRow("table", 12, run, run));
            rows.add(Row.edit("recursive", counters(377, 0, 0, 0), "disjoint", 12, run, 2 * run, 12));
        }
        List<Summary> written = Summary.aggregate(rows);
        List<String> lines = new ArrayList<>();
        lines.add(Summary.HEADER);
        written.forEach(s -> lines.add(s.toCsv()));
        Path file = folder.resolve("summary.csv");
        Files.write(file, lines);

        List<Summary> read = Summary.readCsv(file);
        assertEquals(written.size(), read.size());
        assertEquals("recursive", read.get(1).algorithm());
        assertEquals(377, read.get(1).subproblems());
        assertEquals(0, read.get(1).memoryCells());
    }

    @Test
    void aFileWithAnotherHeaderIsRefused() throws IOException {
        Path file = folder.resolve("other.csv");
        Files.writeString(file, "algorithm,input,n,time_ms\nkmp,random,1,2\n");
        assertThrows(IOException.class, () -> Summary.readCsv(file));
    }
}
