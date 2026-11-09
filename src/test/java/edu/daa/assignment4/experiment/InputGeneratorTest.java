package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.daa.assignment4.experiment.InputGenerator.InputPair;
import edu.daa.assignment4.search.KmpSearch;
import edu.daa.assignment4.search.NaiveSearch;

class InputGeneratorTest {

    private static final long SEED = 42;

    @Test
    void sameSeedGivesTheSameInputs() {
        InputGenerator first = new InputGenerator(SEED);
        InputGenerator second = new InputGenerator(SEED);
        assertEquals(first.editRandom(50), second.editRandom(50));
        assertEquals(first.editDisjoint(50), second.editDisjoint(50));
        assertEquals(first.editIdentical(50), second.editIdentical(50));
        assertEquals(first.searchRandom(1000, 8), second.searchRandom(1000, 8));
        assertEquals(first.searchBestCase(1000, 8), second.searchBestCase(1000, 8));
    }

    @Test
    void differentSeedsGiveDifferentRandomInputs() {
        assertNotEquals(new InputGenerator(1).editRandom(50), new InputGenerator(2).editRandom(50));
        assertNotEquals(new InputGenerator(1).searchRandom(1000, 8), new InputGenerator(2).searchRandom(1000, 8));
    }

    @Test
    void editInputsHaveTheRequestedLength() {
        InputGenerator generator = new InputGenerator(SEED);
        for (InputPair pair : List.of(generator.editIdentical(37), generator.editRandom(37), generator.editDisjoint(37))) {
            assertEquals(37, pair.first().length());
            assertEquals(37, pair.second().length());
        }
    }

    @Test
    void identicalStringsAreIdentical() {
        InputPair pair = new InputGenerator(SEED).editIdentical(100);
        assertEquals(pair.first(), pair.second());
    }

    @Test
    void disjointStringsShareNoCharacter() {
        InputPair pair = new InputGenerator(SEED).editDisjoint(500);
        Set<Character> inFirst = characters(pair.first());
        for (char c : pair.second().toCharArray()) {
            assertFalse(inFirst.contains(c), "shared character " + c);
        }
    }

    @Test
    void bestCaseSearchInputNeverMatchesAndFailsAtOnce() {
        int n = 1000;
        int m = 8;
        InputPair pair = new InputGenerator(SEED).searchBestCase(n, m);
        assertEquals(n, pair.first().length());
        assertEquals(m, pair.second().length());
        assertFalse(pair.first().contains("z"));
        assertEquals('z', pair.second().charAt(0));

        NaiveSearch naive = new NaiveSearch();
        assertTrue(naive.search(pair.first(), pair.second()).isEmpty());
        assertEquals(n - m + 1, naive.counters().comparisons);     // exactly one comparison per start position
    }

    @Test
    void randomSearchInputContainsThePlantedCopies() {
        int n = 1000;
        int m = 8;
        InputPair pair = new InputGenerator(SEED).searchRandom(n, m);
        assertEquals(n, pair.first().length());
        assertEquals(m, pair.second().length());

        int spacing = n / (InputGenerator.PLANTED_COPIES + 1);
        List<Integer> found = new KmpSearch().search(pair.first(), pair.second());
        for (int k = 1; k <= InputGenerator.PLANTED_COPIES; k++) {
            assertTrue(found.contains(k * spacing), "planted copy " + k + " not found, found=" + found);
        }
    }

    @Test
    void worstCaseSearchInputHasNoMatch() {
        InputPair pair = new InputGenerator(SEED).searchWorstCase(1000, 8);
        assertEquals("a".repeat(1000), pair.first());
        assertEquals("aaaaaaab", pair.second());
        assertTrue(new KmpSearch().search(pair.first(), pair.second()).isEmpty());
    }

    @Test
    void allMatchesInputMatchesAtEveryPosition() {
        InputPair pair = new InputGenerator(SEED).searchAllMatches(100, 8);
        assertEquals(100 - 8 + 1, new NaiveSearch().search(pair.first(), pair.second()).size());
        assertEquals(100 - 8 + 1, new KmpSearch().search(pair.first(), pair.second()).size());
    }

    @Test
    void impossibleSizesAreRejected() {
        InputGenerator generator = new InputGenerator(SEED);
        assertThrows(IllegalArgumentException.class, () -> generator.searchWorstCase(5, 6));
        assertThrows(IllegalArgumentException.class, () -> generator.searchAllMatches(5, 0));
        assertThrows(IllegalArgumentException.class, () -> generator.searchRandom(20, 10));   // no room for 5 copies
    }

    private static Set<Character> characters(String s) {
        Set<Character> set = new HashSet<>();
        for (char c : s.toCharArray()) {
            set.add(c);
        }
        return set;
    }
}
