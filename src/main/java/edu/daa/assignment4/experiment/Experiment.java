package edu.daa.assignment4.experiment;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.daa.assignment4.edit.EditDistance;
import edu.daa.assignment4.edit.RecursiveEditDistance;
import edu.daa.assignment4.edit.TableEditDistance;
import edu.daa.assignment4.edit.TwoRowEditDistance;
import edu.daa.assignment4.experiment.InputGenerator.InputPair;
import edu.daa.assignment4.search.KmpSearch;
import edu.daa.assignment4.search.NaiveSearch;
import edu.daa.assignment4.search.StringSearch;

/**
 * Runs the experiment described on the Experiment Design Card and writes
 * <ul>
 *   <li>results/results.csv, one row per timed run, with the counters of that run,</li>
 *   <li>results/summary.csv, one row per case (the median time of its runs and the counters),</li>
 *   <li>results/environment.txt, the machine and the settings,</li>
 *   <li>results/plots/*.png, the plots drawn from the summary, and plots/captions.txt with the sentence under each.</li>
 * </ul>
 *
 * Usage: {@code Experiment [--quick] [--plots-only] [--out FOLDER]}. {@code --quick} runs a tiny version to check that
 * everything works; it writes to target/quick-results unless --out says otherwise, so it never overwrites the real
 * results. {@code --plots-only} redraws the plots from an existing summary.csv without measuring again.
 */
public final class Experiment {

    /** All random inputs derive from this seed, so two runs build exactly the same strings. */
    static final long SEED = 42;

    static final String[] EDIT_INPUTS = {"identical", "random", "disjoint"};
    static final String[] SEARCH_INPUTS = {"best_case", "random", "worst_case", "all_matches"};

    private Experiment() {
    }

    public static void main(String[] args) throws IOException {
        Config config = Config.full();
        Path out = null;
        boolean plotsOnly = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--quick")) {
                config = Config.quick();
            } else if (args[i].equals("--plots-only")) {
                plotsOnly = true;
            } else if (args[i].equals("--out") && i + 1 < args.length) {
                out = Path.of(args[++i]);
            } else {
                System.err.println("Usage: Experiment [--quick] [--plots-only] [--out FOLDER]");
                System.exit(2);
            }
        }
        if (out == null) {
            out = config.name().equals("quick") ? Path.of("target", "quick-results") : Path.of("results");
        }
        if (plotsOnly) {
            redraw(config, out, System.out);
        } else {
            run(config, out, System.out);
        }
    }

    /** Runs the whole experiment and writes all files into the folder. */
    static List<Plots.Plot> run(Config config, Path folder, PrintStream log) throws IOException {
        System.setProperty("java.awt.headless", "true");
        log.println("Experiment '" + config.name() + "': seed " + SEED + ", " + config.runs() + " timed runs per case");
        log.println("Output folder: " + folder.toAbsolutePath().normalize());

        List<Row> rows = measure(config, log);

        Files.createDirectories(folder);
        writeLines(folder.resolve("results.csv"), Row.HEADER, rows.stream().map(Row::toCsv).toList());
        List<Summary> summaries = Summary.aggregate(rows);
        writeLines(folder.resolve("summary.csv"), Summary.HEADER, summaries.stream().map(Summary::toCsv).toList());
        Files.writeString(folder.resolve("environment.txt"), environment(config), StandardCharsets.UTF_8);
        log.println("Wrote " + rows.size() + " rows to results.csv and " + summaries.size() + " cases to summary.csv");

        return drawPlots(summaries, config, folder, log);
    }

    /**
     * Redraws the plots and captions from an existing summary.csv, without measuring anything. Use it when only
     * the look of the plots changes.
     */
    static List<Plots.Plot> redraw(Config config, Path folder, PrintStream log) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Path summaryFile = folder.resolve("summary.csv");
        log.println("Redrawing the plots of configuration '" + config.name() + "' from " + summaryFile.toAbsolutePath().normalize());
        return drawPlots(Summary.readCsv(summaryFile), config, folder, log);
    }

    private static List<Plots.Plot> drawPlots(List<Summary> summaries, Config config, Path folder, PrintStream log)
            throws IOException {
        List<Plots.Plot> plots = Plots.write(summaries, config, folder.resolve("plots"));
        StringBuilder captions = new StringBuilder();
        for (Plots.Plot plot : plots) {
            captions.append(plot.fileName()).append('\n').append(plot.title()).append('\n')
                    .append(plot.caption()).append("\n\n");
            log.println();
            log.println("plots/" + plot.fileName() + ": " + asciiOnly(plot.title()));
            log.println("  " + asciiOnly(plot.caption()));
        }
        Files.writeString(folder.resolve("plots").resolve("captions.txt"), captions, StandardCharsets.UTF_8);
        return plots;
    }

    /** The console may not show the multiplication and division signs of the captions; captions.txt keeps them. */
    private static String asciiOnly(String text) {
        return text.replace(Fmt.TIMES, 'x').replace(Fmt.DIVIDE, '/');
    }

    /** Warms up the JVM, then times every case of the configuration. Writes nothing. */
    static List<Row> measure(Config config, PrintStream log) {
        log.println("Warming up the JVM ...");
        warmUp();

        List<Row> rows = new ArrayList<>();
        runEditDistance(config, rows, log);
        runStringSearch(config, rows, log);

        if (rows.size() != config.expectedRows()) {
            throw new IllegalStateException("Expected " + config.expectedRows() + " rows but measured " + rows.size());
        }
        return rows;
    }

    // ------------------------------------------------------------------ edit distance

    private static void runEditDistance(Config config, List<Row> rows, PrintStream log) {
        EditDistance recursive = new RecursiveEditDistance();
        EditDistance table = new TableEditDistance();
        EditDistance twoRows = new TwoRowEditDistance();

        for (int n : config.editSizes()) {
            List<EditDistance> algorithms = config.isSmall(n)
                    ? List.of(recursive, table, twoRows)
                    : List.of(table, twoRows);
            for (String input : EDIT_INPUTS) {
                InputPair pair = editInput(input, n);
                long agreed = -1;
                for (EditDistance algorithm : algorithms) {
                    List<Row> measured = timeEdit(algorithm, input, pair, n, config.runs());
                    long distance = measured.get(0).result();
                    if (agreed >= 0 && distance != agreed) {
                        throw new IllegalStateException("Edit distance versions disagree on " + input + " n=" + n
                                + ": " + agreed + " vs " + distance + " (" + algorithm.name() + ")");
                    }
                    agreed = distance;
                    rows.addAll(measured);
                    log.println(describe(measured));
                }
            }
        }
    }

    private static List<Row> timeEdit(EditDistance algorithm, String input, InputPair pair, int n, int runs) {
        if (algorithm instanceof RecursiveEditDistance && n > Config.MAX_RECURSIVE_SIZE) {
            throw new IllegalArgumentException("Refusing to run the plain recursion at n = " + n);
        }
        algorithm.distance(pair.first(), pair.second()); // untimed: warms the caches and the JIT for this case
        List<Row> measured = new ArrayList<>();
        for (int run = 1; run <= runs; run++) {
            long start = System.nanoTime();
            int distance = algorithm.distance(pair.first(), pair.second());
            long elapsed = System.nanoTime() - start;
            measured.add(Row.edit(algorithm.name(), algorithm.counters(), input, n, run, toMs(elapsed), distance));
        }
        return measured;
    }

    static InputPair editInput(String input, int n) {
        InputGenerator generator = new InputGenerator(seedFor("edit|" + input + "|" + n));
        switch (input) {
            case "identical":
                return generator.editIdentical(n);
            case "random":
                return generator.editRandom(n);
            case "disjoint":
                return generator.editDisjoint(n);
            default:
                throw new IllegalArgumentException("Unknown edit distance input: " + input);
        }
    }

    // ------------------------------------------------------------------ string search

    private static void runStringSearch(Config config, List<Row> rows, PrintStream log) {
        StringSearch naive = new NaiveSearch();
        StringSearch kmp = new KmpSearch();

        for (int[] shape : config.searchShapes()) {
            int n = shape[0];
            int m = shape[1];
            for (String input : SEARCH_INPUTS) {
                InputPair pair = searchInput(input, n, m);
                List<Integer> naiveMatches = new ArrayList<>();
                List<Row> naiveRows = timeSearch(naive, input, pair, n, m, config.runs(), naiveMatches);
                List<Integer> kmpMatches = new ArrayList<>();
                List<Row> kmpRows = timeSearch(kmp, input, pair, n, m, config.runs(), kmpMatches);
                if (!naiveMatches.equals(kmpMatches)) {
                    throw new IllegalStateException("Naive and KMP found different matches on " + input
                            + " n=" + n + " m=" + m);
                }
                rows.addAll(naiveRows);
                rows.addAll(kmpRows);
                log.println(describe(naiveRows));
                log.println(describe(kmpRows));
            }
        }
    }

    /** Times the runs and leaves the matches of the last run in lastMatches, so the caller can compare algorithms. */
    private static List<Row> timeSearch(StringSearch algorithm, String input, InputPair pair, int n, int m, int runs,
                                        List<Integer> lastMatches) {
        algorithm.search(pair.first(), pair.second()); // untimed
        List<Row> measured = new ArrayList<>();
        for (int run = 1; run <= runs; run++) {
            long start = System.nanoTime();
            List<Integer> matches = algorithm.search(pair.first(), pair.second());
            long elapsed = System.nanoTime() - start;
            measured.add(Row.search(algorithm.name(), algorithm.counters(), input, n, m, run, toMs(elapsed), matches.size()));
            if (run == runs) {
                lastMatches.addAll(matches);
            }
        }
        return measured;
    }

    static InputPair searchInput(String input, int n, int m) {
        InputGenerator generator = new InputGenerator(seedFor("search|" + input + "|" + n + "|" + m));
        switch (input) {
            case "best_case":
                return generator.searchBestCase(n, m);
            case "random":
                return generator.searchRandom(n, m);
            case "worst_case":
                return generator.searchWorstCase(n, m);
            case "all_matches":
                return generator.searchAllMatches(n, m);
            default:
                throw new IllegalArgumentException("Unknown search input: " + input);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Gives every case its own seed, derived from the global seed and the name of the case (SplitMix64 mixing).
     * Adding or removing a case therefore never changes the strings of the other cases.
     */
    static long seedFor(String caseName) {
        long z = SEED + caseName.hashCode() * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Short JIT warm-up of every algorithm, so the first measured case is not compiled during its timing. */
    private static void warmUp() {
        InputGenerator generator = new InputGenerator(seedFor("warmup"));
        EditDistance[] edit = {new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance()};
        for (int i = 0; i < 3; i++) {
            InputPair small = generator.editRandom(9);
            edit[0].distance(small.first(), small.second());
        }
        for (int i = 0; i < 30; i++) {
            InputPair large = generator.editRandom(300);
            edit[1].distance(large.first(), large.second());
            edit[2].distance(large.first(), large.second());
        }
        StringSearch[] search = {new NaiveSearch(), new KmpSearch()};
        for (int i = 0; i < 20; i++) {
            InputPair pair = generator.searchRandom(200_000, 16);
            search[0].search(pair.first(), pair.second());
            search[1].search(pair.first(), pair.second());
        }
    }

    private static double toMs(long nanos) {
        return Math.max(nanos, 1) / 1_000_000.0; // a clock that reads 0 would make a later ratio meaningless
    }

    private static String describe(List<Row> measured) {
        Row first = measured.get(0);
        double[] times = new double[measured.size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = measured.get(i).timeMs();
        }
        String counter = first.family().equals("edit_distance")
                ? "subproblems " + Fmt.full(first.subproblems())
                : "comparisons " + Fmt.full(first.comparisons());
        return String.format(Locale.ROOT, "%-13s %-9s %-11s n=%-8d m=%-4d median %10.3f ms  %s",
                first.family(), first.algorithm(), first.input(), first.n(), first.m(), Stats.median(times), counter);
    }

    private static void writeLines(Path file, String header, List<String> lines) throws IOException {
        StringBuilder text = new StringBuilder(header).append('\n');
        for (String line : lines) {
            text.append(line).append('\n');
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String environment(Config config) {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder text = new StringBuilder();
        text.append("configuration=").append(config.name()).append('\n');
        text.append("seed=").append(SEED).append('\n');
        text.append("timed_runs_per_case=").append(config.runs()).append('\n');
        text.append("java.version=").append(System.getProperty("java.version")).append('\n');
        text.append("java.vm.name=").append(System.getProperty("java.vm.name")).append('\n');
        text.append("java.vendor=").append(System.getProperty("java.vendor")).append('\n');
        text.append("os=").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version"))
                .append(' ').append(System.getProperty("os.arch")).append('\n');
        text.append("available_processors=").append(runtime.availableProcessors()).append('\n');
        text.append("max_heap_mb=").append(runtime.maxMemory() / (1024 * 1024)).append('\n');
        return text.toString();
    }
}
