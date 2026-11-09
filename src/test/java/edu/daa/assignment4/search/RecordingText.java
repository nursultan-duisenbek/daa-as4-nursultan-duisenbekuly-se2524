package edu.daa.assignment4.search;

import java.util.ArrayList;
import java.util.List;

/**
 * A text that remembers the index of every charAt call. It lets a test look at the order in which
 * an algorithm reads the text, which is how "KMP never moves backwards" is checked.
 */
final class RecordingText implements CharSequence {

    private final String text;
    private final List<Integer> reads = new ArrayList<>();

    RecordingText(String text) {
        this.text = text;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        reads.add(index);
        return text.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return text.subSequence(start, end);
    }

    @Override
    public String toString() {
        return text;
    }

    int readCount() {
        return reads.size();
    }

    /** True if no read was at an earlier position than the read before it. */
    boolean neverReadsBackwards() {
        for (int i = 1; i < reads.size(); i++) {
            if (reads.get(i) < reads.get(i - 1)) {
                return false;
            }
        }
        return true;
    }
}
