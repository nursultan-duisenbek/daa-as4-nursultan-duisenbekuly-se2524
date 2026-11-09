package edu.daa.assignment4.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests that both Naive and KMP must pass, so every test runs once per algorithm. */
class StringSearchTest {

    /** Reference matches from section 7 of the assignment. */
    private record Case(String text, String pattern, List<Integer> expected) {
    }

    private static final List<Case> REFERENCE = List.of(
            new Case("ABABDABACDABABCABAB", "ABABCABAB", List.of(10)),
            new Case("AAAAA", "AA", List.of(0, 1, 2, 3)),
            new Case("ABABABA", "ABA", List.of(0, 2, 4)));

    static Stream<StringSearch> algorithms() {
        return Stream.of(new NaiveSearch(), new KmpSearch());
    }

    static Stream<Arguments> referenceCases() {
        return algorithms().flatMap(s -> REFERENCE.stream()
                .map(c -> Arguments.of(s, c.text(), c.pattern(), c.expected())));
    }

    @ParameterizedTest(name = "{0}: search(\"{1}\", \"{2}\") = {3}")
    @MethodSource("referenceCases")
    void referenceMatches(StringSearch search, String text, String pattern, List<Integer> expected) {
        assertEquals(expected, search.search(text, pattern));
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void overlappingMatchesAreAllReported(StringSearch search) {
        assertEquals(List.of(0, 1), search.search("aaaa", "aaa"));
        assertEquals(List.of(0, 2, 4), search.search("abababab", "abab"));
        assertEquals(List.of(0, 3), search.search("abcabc", "abc"));
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void noMatchGivesAnEmptyList(StringSearch search) {
        assertTrue(search.search("ABCDEF", "XYZ").isEmpty());
        assertTrue(search.search("AAAAAA", "AAB").isEmpty());
        assertTrue(search.search("", "A").isEmpty());
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void emptyPatternThrows(StringSearch search) {
        assertThrows(IllegalArgumentException.class, () -> search.search("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> search.search("", ""));
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void patternLongerThanTextGivesAnEmptyList(StringSearch search) {
        assertTrue(search.search("ab", "abc").isEmpty());
        assertTrue(search.search("", "abc").isEmpty());
        assertTrue(search.search("a", "aa").isEmpty());
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void shortAndBoundaryCases(StringSearch search) {
        assertEquals(List.of(0), search.search("abc", "abc"));      // pattern equals text
        assertEquals(List.of(0), search.search("a", "a"));
        assertEquals(List.of(1, 3, 5), search.search("banana", "a"));
        assertEquals(List.of(0, 3), search.search("abcab", "ab"));   // match at both ends
    }

    @Test
    void hundredRandomTextsGiveTheSameListInNaiveAndKmp() {
        Random random = new Random(2524);
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();

        for (int k = 0; k < 100; k++) {
            // Small alphabets make lots of matches, many of them overlapping.
            String alphabet = k % 2 == 0 ? "ab" : "abc";
            String text = randomString(random, random.nextInt(61), alphabet);          // length 0..60
            String pattern = randomString(random, 1 + random.nextInt(6), alphabet);    // length 1..6
            String where = "text=\"" + text + "\", pattern=\"" + pattern + "\"";

            List<Integer> expected = indexOfOracle(text, pattern);
            assertEquals(expected, naive.search(text, pattern), "naive, " + where);
            assertEquals(expected, kmp.search(text, pattern), "kmp, " + where);
        }
    }

    /** Third opinion that uses neither algorithm: String.indexOf, restarting one position after each hit. */
    private static List<Integer> indexOfOracle(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        for (int at = text.indexOf(pattern); at >= 0; at = text.indexOf(pattern, at + 1)) {
            hits.add(at);
        }
        return hits;
    }

    private static String randomString(Random random, int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
