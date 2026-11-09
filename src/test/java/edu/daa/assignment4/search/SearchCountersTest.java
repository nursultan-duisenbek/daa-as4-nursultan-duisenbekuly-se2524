package edu.daa.assignment4.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Small hand-checked examples that show what each search counter really counts. */
class SearchCountersTest {

    @Test
    void naiveCountsEveryCharacterComparison() {
        NaiveSearch naive = new NaiveSearch();

        naive.search("AAAAA", "AA");        // 4 start positions, 2 successful comparisons each
        assertEquals(8, naive.counters().comparisons);

        naive.search("ABCD", "X");          // 4 start positions, 1 failed comparison each
        assertEquals(4, naive.counters().comparisons);

        naive.search("ab", "abc");          // pattern longer than text: nothing is compared
        assertEquals(0, naive.counters().comparisons);

        assertEquals(0, naive.counters().lpsComparisons);
        assertEquals(0, naive.counters().memoryCells);
    }

    @Test
    void kmpCountsSearchAndLpsComparisonsSeparately() {
        KmpSearch kmp = new KmpSearch();

        kmp.search("AAAAA", "AA");
        assertEquals(5, kmp.counters().comparisons);       // one comparison per text character
        assertEquals(1, kmp.counters().lpsComparisons);    // only p[1] against p[0]
        assertEquals(2, kmp.counters().memoryCells);       // the LPS array has m = 2 cells

        kmp.search("ABCD", "X");
        assertEquals(4, kmp.counters().comparisons);
        assertEquals(0, kmp.counters().lpsComparisons);    // a one-character pattern needs no comparison
    }

    @Test
    void countersStartFromZeroOnEveryCall() {
        NaiveSearch naive = new NaiveSearch();
        naive.search("ABABABA", "ABA");
        long first = naive.counters().comparisons;
        naive.search("ABABABA", "ABA");
        assertEquals(first, naive.counters().comparisons);

        KmpSearch kmp = new KmpSearch();
        kmp.search("ABABABA", "ABA");
        long firstKmp = kmp.counters().comparisons;
        kmp.search("ABABABA", "ABA");
        assertEquals(firstKmp, kmp.counters().comparisons);
    }
}
