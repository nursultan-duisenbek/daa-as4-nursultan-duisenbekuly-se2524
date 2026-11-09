package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class FmtTest {

    @Test
    void fullGroupsThousands() {
        assertEquals("1,234,567", Fmt.full(1_234_567));
        assertEquals("377,393,953", Fmt.full(377_393_953L));
        assertEquals("12", Fmt.full(12));
    }

    @Test
    void compactKeepsThreeSignificantDigits() {
        assertEquals("377M", Fmt.compact(377_393_953));
        assertEquals("2.11B", Fmt.compact(2_110_000_000.0));
        assertEquals("13.5K", Fmt.compact(13_483));
        assertEquals("256", Fmt.compact(255.93));
        assertEquals("1.04", Fmt.compact(1.04));
        assertEquals("0.5", Fmt.compact(0.5));
        assertEquals("2", Fmt.compact(2.0));
    }

    @Test
    void ticksAreWrittenInFullOrCompact() {
        assertEquals("1,000", Fmt.tick(1000, false));
        assertEquals("1K", Fmt.tick(1000, true));
        assertEquals("100M", Fmt.tick(100_000_000, true));
        assertEquals("2M", Fmt.tick(2_000_000, true));
        assertEquals("125K", Fmt.tick(125_000, true));
        assertEquals("0.5", Fmt.tick(0.5, false));
        assertEquals("5", Fmt.tick(5, true));
    }

    @Test
    void ratiosCarryTheMultiplicationSign() {
        assertEquals("0.5" + Fmt.TIMES, Fmt.ratioTick(0.5));
        assertEquals("1,000" + Fmt.TIMES, Fmt.ratioTick(1000));
        assertEquals("1.23" + Fmt.TIMES, Fmt.times(1.2345));
        assertEquals("23.1" + Fmt.TIMES, Fmt.times(23.14));
    }

    @Test
    void twoDecimalsAreAlwaysShown() {
        assertEquals("1.00", Fmt.two(1.0));
        assertEquals("255.93", Fmt.two(255.934));
    }

    @Test
    void millisecondsKeepThreeSignificantDigits() {
        assertEquals("0.00432", Fmt.ms(0.004321));
        assertEquals("12.3", Fmt.ms(12.345));
        assertEquals("1230", Fmt.ms(1234.5));
        assertEquals("0", Fmt.ms(0));
    }

    @Test
    void outputDoesNotDependOnTheDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY); // decimal comma, dot as thousands separator
            assertEquals("1.04", Fmt.compact(1.04));
            assertEquals("1,234,567", Fmt.full(1_234_567));
            assertEquals("255.93", Fmt.two(255.934));
            assertEquals("0.00432", Fmt.ms(0.004321));
        } finally {
            Locale.setDefault(saved);
        }
    }
}
