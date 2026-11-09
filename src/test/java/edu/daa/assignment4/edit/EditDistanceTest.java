package edu.daa.assignment4.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EditDistanceTest {

    /** Reference values from section 7 of the assignment. */
    private record Case(String a, String b, int expected) {
    }

    private static final List<Case> REFERENCE = List.of(
            new Case("kitten", "sitting", 3),
            new Case("flaw", "lawn", 2),
            new Case("intention", "execution", 5),
            new Case("", "abc", 3),
            new Case("abc", "abc", 0));

    /** A fresh object of each version, so one test never sees counters left over by another. */
    static Stream<EditDistance> versions() {
        return Stream.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
    }

    static Stream<Arguments> referenceCases() {
        return versions().flatMap(v -> REFERENCE.stream()
                .map(c -> Arguments.of(v, c.a(), c.b(), c.expected())));
    }

    @ParameterizedTest(name = "{0}: distance(\"{1}\", \"{2}\") = {3}")
    @MethodSource("referenceCases")
    void referenceValues(EditDistance version, String a, String b, int expected) {
        assertEquals(expected, version.distance(a, b));
    }

    @ParameterizedTest
    @MethodSource("versions")
    void emptyStringsWork(EditDistance version) {
        assertEquals(0, version.distance("", ""));
        assertEquals(3, version.distance("abc", ""));
        assertEquals(3, version.distance("", "abc"));
        assertEquals(1, version.distance("a", ""));
        assertEquals(1, version.distance("", "z"));
    }

    @Test
    void hundredRandomPairsGiveTheSameNumberInAllVersions() {
        Random random = new Random(2524);
        EditDistance recursive = new RecursiveEditDistance();
        EditDistance table = new TableEditDistance();
        EditDistance twoRows = new TwoRowEditDistance();

        for (int k = 0; k < 100; k++) {
            // Alternate between a tiny alphabet (many equal characters) and a wider one (few).
            String alphabet = k % 2 == 0 ? "ab" : "abcdefghij";
            String a = randomString(random, random.nextInt(9), alphabet);   // length 0..8, so the recursion stays cheap
            String b = randomString(random, random.nextInt(9), alphabet);
            String where = "a=\"" + a + "\", b=\"" + b + "\"";

            int expected = ReferenceEditDistance.distance(a, b);
            assertEquals(expected, recursive.distance(a, b), "recursive, " + where);
            assertEquals(expected, table.distance(a, b), "table, " + where);
            assertEquals(expected, twoRows.distance(a, b), "two rows, " + where);

            // Swapping the strings must not change the answer.
            assertEquals(expected, recursive.distance(b, a), "recursive swapped, " + where);
            assertEquals(expected, table.distance(b, a), "table swapped, " + where);
            assertEquals(expected, twoRows.distance(b, a), "two rows swapped, " + where);
        }
    }

    @Test
    void tableAndTwoRowsFillEveryCellOnceAndShowTheMemoryDifference() {
        String a = "kitten";   // n = 6
        String b = "sitting";  // m = 7

        TableEditDistance table = new TableEditDistance();
        table.distance(a, b);
        assertEquals(7 * 8, table.counters().subproblems);   // (n+1) * (m+1) cells
        assertEquals(7 * 8, table.counters().memoryCells);

        TwoRowEditDistance twoRows = new TwoRowEditDistance();
        twoRows.distance(a, b);
        assertEquals(7 * 8, twoRows.counters().subproblems); // same work as the table
        assertEquals(2 * 8, twoRows.counters().memoryCells); // but only two rows of m+1 cells
    }

    @Test
    void recursionCountsEveryCallIncludingRepeatedOnes() {
        RecursiveEditDistance recursive = new RecursiveEditDistance();

        recursive.distance("", "");
        assertEquals(1, recursive.counters().subproblems);

        recursive.distance("a", "b");        // f(1,1) calls f(0,1), f(1,0) and f(0,0)
        assertEquals(4, recursive.counters().subproblems);

        recursive.distance("abc", "abc");    // identical strings: only the diagonal, n+1 calls
        assertEquals(4, recursive.counters().subproblems);

        recursive.distance("aaa", "bbb");    // no equal characters: every call branches three ways
        assertEquals(94, recursive.counters().subproblems);

        assertEquals(0, recursive.counters().memoryCells);   // no table at all
    }

    @ParameterizedTest
    @MethodSource("versions")
    void countersStartFromZeroOnEveryCall(EditDistance version) {
        version.distance("kitten", "sitting");
        long first = version.counters().subproblems;
        version.distance("kitten", "sitting");
        assertEquals(first, version.counters().subproblems);
    }

    private static String randomString(Random random, int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
