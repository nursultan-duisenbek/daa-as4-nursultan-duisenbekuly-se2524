package edu.daa.assignment4.experiment;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import edu.daa.assignment4.edit.RecursiveEditDistance;
import edu.daa.assignment4.experiment.InputGenerator.InputPair;

/**
 * Follow-up to the main experiment, added after the first full run (section 7 of the report explains why).
 *
 * The main experiment measures one random pair of strings per size. The number of calls of the plain recursion
 * depends strongly on which characters happen to match, so a single pair can be far from typical. This class
 * counts the calls of the real recursive version on many other random pairs of the same length and writes one
 * row per pair to random_spread.csv. It records counters only and no times, so it needs no repeated runs.
 *
 * Usage: {@code RandomSpread [--n N] [--pairs P] [--out FOLDER]}. The defaults are n = 12 (the size the card makes
 * its prediction about), 100 pairs and the folder results.
 */
public final class RandomSpread {

    static final String HEADER = "n,pair,subproblems,disjoint_subproblems,share_of_disjoint";
    static final int DEFAULT_N = 12;
    static final int DEFAULT_PAIRS = 100;

    /**
     * What one run counted.
     *
     * @param n        length of both strings
     * @param disjoint calls on the disjoint pair of the main experiment (no characters match, the worst case)
     * @param mainPair calls on the random pair of the main experiment, the one in results.csv
     * @param counts   calls on each of the other random pairs, in the order of the pair numbers
     */
    record Result(int n, long disjoint, long mainPair, long[] counts) {
        double share(long calls) {
            return calls / (double) disjoint;
        }
    }

    private RandomSpread() {
    }

    public static void main(String[] args) throws IOException {
        int n = DEFAULT_N;
        int pairs = DEFAULT_PAIRS;
        Path out = Path.of("results");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--n") && i + 1 < args.length) {
                n = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--pairs") && i + 1 < args.length) {
                pairs = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--out") && i + 1 < args.length) {
                out = Path.of(args[++i]);
            } else {
                System.err.println("Usage: RandomSpread [--n N] [--pairs P] [--out FOLDER]");
                System.exit(2);
            }
        }
        run(n, pairs, out, System.out);
    }

    /** Counts the calls on the reference pairs and on {@code pairs} other random pairs, writes the CSV and a summary. */
    static Result run(int n, int pairs, Path folder, PrintStream log) throws IOException {
        if (n < 1 || n > Config.MAX_RECURSIVE_SIZE) {
            throw new IllegalArgumentException("n must be between 1 and " + Config.MAX_RECURSIVE_SIZE
                    + " because the plain recursion is exponential, got " + n);
        }
        if (pairs < 1) {
            throw new IllegalArgumentException("Need at least one pair, got " + pairs);
        }
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        long disjoint = calls(recursive, Experiment.editInput("disjoint", n));
        long mainPair = calls(recursive, Experiment.editInput("random", n));

        long[] counts = new long[pairs];
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (int pair = 1; pair <= pairs; pair++) {
            InputPair strings = new InputGenerator(Experiment.seedFor("edit|spread|" + n + "|" + pair)).editRandom(n);
            counts[pair - 1] = calls(recursive, strings);
            csv.append(n).append(',').append(pair).append(',').append(counts[pair - 1]).append(',').append(disjoint)
                    .append(',').append(String.format(Locale.ROOT, "%.4f", counts[pair - 1] / (double) disjoint)).append('\n');
        }
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("random_spread.csv"), csv, StandardCharsets.UTF_8);

        Result result = new Result(n, disjoint, mainPair, counts);
        summarize(result, folder.resolve("random_spread.csv"), log);
        return result;
    }

    private static long calls(RecursiveEditDistance recursive, InputPair pair) {
        recursive.distance(pair.first(), pair.second());
        return recursive.counters().subproblems;
    }

    private static void summarize(Result result, Path file, PrintStream log) {
        double[] shares = new double[result.counts().length];
        long fewer = 0;
        double sum = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] = result.share(result.counts()[i]);
            sum += shares[i];
            if (result.counts()[i] < result.mainPair()) {
                fewer++;
            }
        }
        log.println("Plain recursion, n = " + result.n() + ", " + shares.length + " other random pairs, counters only");
        log.println("  disjoint pair (worst case): " + Fmt.full(result.disjoint()) + " calls");
        log.println("  random pair of the main experiment: " + Fmt.full(result.mainPair()) + " calls = "
                + percent(result.share(result.mainPair())) + " of the disjoint count");
        log.println("  other random pairs, as a share of the disjoint count:");
        log.println("    min " + percent(Stats.percentile(shares, 0))
                + "   5th percentile " + percent(Stats.percentile(shares, 5))
                + "   median " + percent(Stats.median(shares))
                + "   mean " + percent(sum / shares.length)
                + "   95th percentile " + percent(Stats.percentile(shares, 95))
                + "   max " + percent(Stats.percentile(shares, 100)));
        log.println("  other pairs with fewer calls than the main experiment's pair: " + fewer + " of " + shares.length);
        log.println("Wrote " + file.toAbsolutePath().normalize());
    }

    private static String percent(double share) {
        return String.format(Locale.ROOT, "%.1f%%", 100 * share);
    }
}
