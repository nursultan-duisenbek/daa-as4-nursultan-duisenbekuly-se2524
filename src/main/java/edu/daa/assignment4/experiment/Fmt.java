package edu.daa.assignment4.experiment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

/**
 * Number formatting for axis ticks, labels and captions. The locale is fixed so the output is the same on every
 * machine. The few non-ASCII symbols are written as char codes so the source does not depend on the file encoding.
 */
final class Fmt {

    /** The multiplication sign, used for ratios such as 12.5x. */
    static final char TIMES = (char) 0xD7;
    /** The division sign, used in axis titles such as "Naive time / KMP time". */
    static final char DIVIDE = (char) 0xF7;

    private Fmt() {
    }

    /** 1,234,567 */
    static String full(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    /** A value with at most three significant digits and a K, M or B suffix: 377M, 2.11B, 12K, 1.04, 0.5 */
    static String compact(double value) {
        double a = Math.abs(value);
        if (a >= 1e9) {
            return trim(value / 1e9, a >= 1e11 ? 0 : a >= 1e10 ? 1 : 2) + "B";
        }
        if (a >= 1e6) {
            return trim(value / 1e6, a >= 1e8 ? 0 : a >= 1e7 ? 1 : 2) + "M";
        }
        if (a >= 1e4) {
            return trim(value / 1e3, a >= 1e5 ? 0 : 1) + "K";
        }
        if (a >= 100) {
            return trim(value, 0);
        }
        if (a >= 10) {
            return trim(value, 1);
        }
        return trim(value, 2);
    }

    /**
     * Axis tick for a value of the form 1, 2 or 5 times a power of ten: written in full for a plain axis and
     * with a K, M or B suffix for a compact one. Values below 1 keep their decimals (0.5, 0.2).
     */
    static String tick(double value, boolean compactAxis) {
        if (value < 1) {
            return trim(value, 2);
        }
        if (!compactAxis) {
            return full(Math.round(value));
        }
        if (value >= 1e9) {
            return trim(value / 1e9, 0) + "B";
        }
        if (value >= 1e6) {
            return trim(value / 1e6, 0) + "M";
        }
        if (value >= 1e3) {
            return trim(value / 1e3, 0) + "K";
        }
        return trim(value, 0);
    }

    /** A ratio tick such as 0.5x, 1x or 1,000x. */
    static String ratioTick(double value) {
        return tick(value, false) + TIMES;
    }

    /** A ratio such as 23.1x or 1.04x. */
    static String times(double ratio) {
        return compact(ratio) + TIMES;
    }

    /** A value with exactly two decimals, 255.93 */
    static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** A time in milliseconds rounded to three significant digits: 0.00432, 12.3, 1230 */
    static String ms(double value) {
        if (value == 0) {
            return "0";
        }
        return new BigDecimal(value).round(new MathContext(3)).stripTrailingZeros().toPlainString();
    }

    private static String trim(double value, int decimals) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", value);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
