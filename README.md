# DAA Assignment 4: edit distance and string search

Nursultan Duisenbekuly, group SE-2524.

The project has three edit distance versions (recursive, table, two rows), Naive and KMP string search, counters that the algorithms fill while they run, a seeded input generator, an experiment runner that writes the CSV files and the plots, and JUnit 5 tests. `REPORT.md` has the Experiment Design Card, the plots, prediction against result and the short answers. `REPORT.pdf` is the same report printed on four A4 pages.

## What you need

JDK 17 or newer and Maven 3.6.3 or newer. The plots are drawn with Java's own 2D classes, so no charting library is downloaded. On a minimal Linux install the JDK may need fontconfig and a font package (for example `fonts-dejavu-core`) to draw text. Without fonts the tests that draw charts are skipped, and the experiment cannot write plots.

## Build

Run everything from the project folder.

```
mvn clean package
```

This compiles the code, runs all tests and builds `target/daa-assignment4-1.0.jar`.

## Run the tests

```
mvn test
```

The tests check the reference values of the assignment (kitten and sitting, the LPS arrays, the search matches), the three edit distance versions against each other on 100 random pairs, Naive against KMP on 100 random texts, empty and too-long patterns, that KMP never reads the text backwards, the counters against their formulas, that the generator is deterministic, and that a quick version of the experiment writes its files and plots.

## Run the experiment

```
mvn -q compile exec:java
```

The full experiment takes about half a minute and writes into `results/`:

| File | Content |
|---|---|
| `results/results.csv` | One row per timed run (820 rows): `family, algorithm, input, n, m, run, time_ms, result, subproblems, memory_cells, comparisons, lps_comparisons`. An empty field means the counter does not apply to that algorithm. For edit distance m is equal to n. |
| `results/summary.csv` | One row per case (164 rows): median, minimum and maximum time of its runs, and the counters. |
| `results/environment.txt` | Java version, machine, seed and number of runs. |
| `results/plots/*.png` | The four plots. `captions.txt` holds the sentence under each plot. |
| `results/random_spread.csv` | Follow-up described in section 7 of the report: recursion calls on 100 other random pairs at n = 12. |
| `results/repeat_run/` | A second full run, kept to show how much times differ between two runs of the same command. |

Other commands, after `mvn compile` or `mvn package`:

```
mvn -q compile exec:java -Dexec.args="--quick"
java -cp target/classes edu.daa.assignment4.experiment.Experiment --plots-only
java -cp target/classes edu.daa.assignment4.experiment.RandomSpread
```

`--quick` runs a tiny version that only checks the pipeline and writes to `target/quick-results`. `--plots-only` draws the plots again from `results/summary.csv` without measuring. `RandomSpread` is the follow-up and takes about 40 seconds. Both programs accept `--out FOLDER` to write somewhere else.

A full run overwrites `results/results.csv`, `results/summary.csv` and the plots with new times. The numbers in `REPORT.md` describe the files that are in this repository, so pass `--out` (for example `-Dexec.args="--out target/my-run"`) if you only want to try it.

## Reproducibility

The seed is 42. Each case derives its own seed from it, so the same command builds the same strings and gives the same counters on every machine. Times depend on the machine and change from run to run. Each case is timed five times after a warm-up and the plots use the median.

## AI-use acknowledgement

I used an AI assistant (Claude, by Anthropic, through Claude Code) to build this project from the assignment text. It wrote the Java code, the tests, the experiment runner, the plot code, the report and this README, and it ran the tests and the experiment. The numbers in `results/` and `REPORT.md` come from running the code in this repository, not from typing them in. The Experiment Design Card was committed before the first run, and the git history shows the order.
