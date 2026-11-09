package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.daa.assignment4.edit.EditDistance;
import edu.daa.assignment4.edit.RecursiveEditDistance;
import edu.daa.assignment4.edit.TableEditDistance;
import edu.daa.assignment4.edit.TwoRowEditDistance;
import edu.daa.assignment4.experiment.InputGenerator.InputPair;
import edu.daa.assignment4.search.KmpSearch;
import edu.daa.assignment4.search.NaiveSearch;

/**
 * Checks, on small sizes, the exact counter values worked out by hand for each experiment input.
 * These are the formulas behind the predictions in REPORT.md. They are properties of the code,
 * so they must hold before any experiment is run. The counters themselves are still measured
 * inside the algorithms, never computed from these formulas.
 */
class CounterFormulasTest {

    /** D(i, j) with D(i, 0) = D(0, j) = 1 and D(i, j) = D(i-1, j) + D(i, j-1) + D(i-1, j-1); returns D(n, n). */
    private static long delannoy(int n) {
        long[][] d = new long[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                d[i][j] = (i == 0 || j == 0) ? 1 : d[i - 1][j] + d[i][j - 1] + d[i - 1][j - 1];
            }
        }
        return d[n][n];
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
    void recursiveCallsOnTheWorstCaseInputFollowTheDelannoyFormula(int n) {
        InputPair pair = new InputGenerator(1).editDisjoint(n);
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        recursive.distance(pair.first(), pair.second());
        assertEquals((3 * delannoy(n) - 1) / 2, recursive.counters().subproblems);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void recursiveCallsOnIdenticalStringsAreNPlusOne(int n) {
        InputPair pair = new InputGenerator(1).editIdentical(n);
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        recursive.distance(pair.first(), pair.second());
        assertEquals(n + 1, recursive.counters().subproblems);
    }

    @Test
    void randomStringsCostBetweenTheBestAndTheWorstCase() {
        int n = 8;
        InputGenerator generator = new InputGenerator(1);
        long best = callsOf(generator.editIdentical(n));
        long typical = callsOf(generator.editRandom(n));
        long worst = callsOf(generator.editDisjoint(n));
        assertTrue(best <= typical && typical <= worst, best + " <= " + typical + " <= " + worst);
    }

    @Test
    void tableAndTwoRowsDoTheSameWorkOnEveryInput() {
        int n = 30;
        InputGenerator generator = new InputGenerator(1);
        for (InputPair pair : new InputPair[] {generator.editIdentical(n), generator.editRandom(n), generator.editDisjoint(n)}) {
            for (EditDistance version : new EditDistance[] {new TableEditDistance(), new TwoRowEditDistance()}) {
                version.distance(pair.first(), pair.second());
                assertEquals((n + 1) * (n + 1), version.counters().subproblems, version.name());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000})
    void memoryOfTableGrowsLikeNSquaredAndTwoRowsLikeN(int n) {
        InputPair pair = new InputGenerator(1).editRandom(n);
        TableEditDistance table = new TableEditDistance();
        TwoRowEditDistance twoRows = new TwoRowEditDistance();
        table.distance(pair.first(), pair.second());
        twoRows.distance(pair.first(), pair.second());
        assertEquals((long) (n + 1) * (n + 1), table.counters().memoryCells);
        assertEquals(2L * (n + 1), twoRows.counters().memoryCells);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 64})
    void naiveSearchComparisonsOnEachInput(int m) {
        int n = 2000;
        long starts = n - m + 1;
        InputGenerator generator = new InputGenerator(1);
        NaiveSearch naive = new NaiveSearch();

        InputPair best = generator.searchBestCase(n, m);
        naive.search(best.first(), best.second());
        assertEquals(starts, naive.counters().comparisons);

        InputPair worst = generator.searchWorstCase(n, m);
        naive.search(worst.first(), worst.second());
        assertEquals(starts * m, naive.counters().comparisons);

        InputPair all = generator.searchAllMatches(n, m);
        naive.search(all.first(), all.second());
        assertEquals(starts * m, naive.counters().comparisons);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 64})
    void kmpSearchComparisonsOnEachInput(int m) {
        int n = 2000;
        InputGenerator generator = new InputGenerator(1);
        KmpSearch kmp = new KmpSearch();

        InputPair best = generator.searchBestCase(n, m);
        kmp.search(best.first(), best.second());
        assertEquals(n, kmp.counters().comparisons);

        InputPair worst = generator.searchWorstCase(n, m);
        kmp.search(worst.first(), worst.second());
        assertEquals(2L * n - m + 1, kmp.counters().comparisons);   // the 2n bound, almost reached
        assertEquals(2L * m - 3, kmp.counters().lpsComparisons);

        InputPair all = generator.searchAllMatches(n, m);
        kmp.search(all.first(), all.second());
        assertEquals(n, kmp.counters().comparisons);
        assertEquals(m - 1, kmp.counters().lpsComparisons);
    }

    private static long callsOf(InputPair pair) {
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        recursive.distance(pair.first(), pair.second());
        return recursive.counters().subproblems;
    }
}
