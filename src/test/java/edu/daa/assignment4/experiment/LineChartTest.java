package edu.daa.assignment4.experiment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.daa.assignment4.experiment.LineChart.Marker;
import edu.daa.assignment4.experiment.LineChart.Point;
import edu.daa.assignment4.experiment.LineChart.Series;

class LineChartTest {

    @TempDir
    Path folder;

    @BeforeAll
    static void onlyWhereJavaCanDrawText() {
        Assumptions.assumeTrue(canDrawText(), "this Java installation cannot draw text (no fonts), so charts cannot be rendered");
    }

    static boolean canDrawText() {
        try {
            System.setProperty("java.awt.headless", "true");
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                return g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).stringWidth("n") > 0;
            } finally {
                g.dispose();
            }
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ label spreading

    @Test
    void labelsThatAlreadyHaveRoomStayWhereTheyAre() {
        double[] placed = LineChart.spread(new double[]{40, 100, 300}, 17, 0, 1000);
        assertArrayEquals(new double[]{40, 100, 300}, placed, 1e-9);
    }

    @Test
    void overlappingLabelsAreSeparatedAroundTheirMeanAndKeepTheirOrder() {
        double[] placed = LineChart.spread(new double[]{101, 100, 102}, 17, 0, 1000);
        // Wanted order by position: 100 (index 1), 101 (index 0), 102 (index 2).
        assertEquals(101 - 17, placed[1], 1e-9);
        assertEquals(101, placed[0], 1e-9);
        assertEquals(101 + 17, placed[2], 1e-9);
    }

    @Test
    void separatedLabelsAreAtLeastTheGapApart() {
        double[] wanted = {10, 12, 14, 16, 200, 203};
        double[] placed = LineChart.spread(wanted, 17, 0, 1000);
        double[] sorted = placed.clone();
        java.util.Arrays.sort(sorted);
        for (int i = 1; i < sorted.length; i++) {
            assertTrue(sorted[i] - sorted[i - 1] >= 17 - 1e-9, "labels " + (i - 1) + " and " + i + " overlap");
        }
    }

    @Test
    void labelsAreKeptInsideTheGivenRange() {
        double[] atTop = LineChart.spread(new double[]{0, 1}, 17, 10, 500);
        assertEquals(10, atTop[0], 1e-9);
        assertEquals(27, atTop[1], 1e-9);
        double[] atBottom = LineChart.spread(new double[]{499, 500}, 17, 10, 500);
        assertEquals(500, atBottom[1], 1e-9);
        assertEquals(483, atBottom[0], 1e-9);
    }

    @Test
    void spreadOfNoLabelsIsEmpty() {
        assertEquals(0, LineChart.spread(new double[0], 17, 0, 100).length);
    }

    // ------------------------------------------------------------------ axes

    @Test
    void snapMovesToTheNearestOneTwoFiveValue() {
        assertEquals(0.5, LineChart.snap125(0.91, false), 1e-12);
        assertEquals(500, LineChart.snap125(281, true), 1e-9);
        assertEquals(100, LineChart.snap125(100, true), 1e-9);
        assertEquals(100, LineChart.snap125(100, false), 1e-9);
        assertEquals(5e8, LineChart.snap125(4.15e8, true), 1);
        assertEquals(2, LineChart.snap125(2.7, false), 1e-12);
    }

    @Test
    void logTicksUsePowersOfTenWhenThereAreEnoughOfThem() {
        List<Double> ticks = LineChart.logTicks(2, 5e8);
        assertEquals(8, ticks.size());
        assertEquals(10.0, ticks.get(0), 1e-9);
        assertEquals(1e8, ticks.get(7), 1);
    }

    @Test
    void logTicksAddOneTwoFiveStepsOnAShortRange() {
        List<Double> ticks = LineChart.logTicks(0.5, 500);
        assertEquals(List.of(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0), ticks);
    }

    // ------------------------------------------------------------------ rendering

    private static Series line(String name, Color color, Marker marker, boolean dashed, double... xy) {
        java.util.ArrayList<Point> points = new java.util.ArrayList<>();
        for (int i = 0; i < xy.length; i += 2) {
            points.add(Double.isNaN(xy[i]) ? Point.gap() : new Point(xy[i], xy[i + 1]));
        }
        return new Series(name, name, color, marker, dashed, points);
    }

    private static int countNear(BufferedImage image, Color color, int tolerance) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y));
                if (Math.abs(c.getRed() - color.getRed()) <= tolerance && Math.abs(c.getGreen() - color.getGreen()) <= tolerance
                        && Math.abs(c.getBlue() - color.getBlue()) <= tolerance) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void chartIsWrittenAsAPngThatContainsEverySeriesColour() throws IOException {
        LineChart chart = new LineChart("A title", "x axis (units)", "y axis (units)")
                .linearX(1, 2, 3, 4)
                .caption("One sentence under the plot.")
                .footnote("Source: a test.")
                .add(line("first", LineChart.BLUE, Marker.CIRCLE, false, 1, 10, 2, 100, 3, 1000, 4, 10_000))
                .add(line("second", LineChart.ORANGE, Marker.SQUARE, true, 1, 5, 2, 50, 3, 500, 4, 5000));
        Path file = folder.resolve("nested").resolve("chart.png");
        chart.write(file);

        BufferedImage image = ImageIO.read(file.toFile());
        assertEquals(LineChart.IMAGE_WIDTH, image.getWidth());
        assertTrue(image.getHeight() > 600, "height " + image.getHeight());
        assertEquals(0xfcfcfb, image.getRGB(2, 2) & 0xffffff, "background");
        assertTrue(countNear(image, LineChart.BLUE, 6) > 500, "blue series is drawn");
        assertTrue(countNear(image, LineChart.ORANGE, 6) > 500, "orange series is drawn");
        assertEquals(0, countNear(image, LineChart.AQUA, 6), "a colour that no series uses must not appear");
    }

    @Test
    void aGapBreaksTheLineButTheSeriesIsStillDrawn() throws IOException {
        LineChart chart = new LineChart("Gap", "n (log scale)", "ratio (log scale)")
                .logX()
                .reference(1, "1x")
                .add(line("with gap", LineChart.AQUA, Marker.DIAMOND, false, 2, 1.5, 4, 2, Double.NaN, Double.NaN, 250, 3, 500, 4))
                .add(line("other", LineChart.YELLOW, Marker.TRIANGLE, false, 2, 1, 500, 1));
        Path file = folder.resolve("gap.png");
        chart.write(file);
        BufferedImage image = ImageIO.read(file.toFile());
        assertTrue(countNear(image, LineChart.AQUA, 6) > 300);
        assertTrue(countNear(image, LineChart.YELLOW, 6) > 300);
    }

    @Test
    void aLongCaptionMakesTheImageTaller() throws IOException {
        LineChart shortCaption = new LineChart("T", "x", "y").linearX(1, 2)
                .caption("Short.")
                .add(line("a", LineChart.BLUE, Marker.CIRCLE, false, 1, 1, 2, 2))
                .add(line("b", LineChart.ORANGE, Marker.CIRCLE, false, 1, 2, 2, 3));
        LineChart longCaption = new LineChart("T", "x", "y").linearX(1, 2)
                .caption(("This caption is deliberately long so that it has to wrap onto several lines. ").repeat(6))
                .add(line("a", LineChart.BLUE, Marker.CIRCLE, false, 1, 1, 2, 2))
                .add(line("b", LineChart.ORANGE, Marker.CIRCLE, false, 1, 2, 2, 3));
        shortCaption.write(folder.resolve("short.png"));
        longCaption.write(folder.resolve("long.png"));
        int shortHeight = ImageIO.read(folder.resolve("short.png").toFile()).getHeight();
        int longHeight = ImageIO.read(folder.resolve("long.png").toFile()).getHeight();
        assertTrue(longHeight > shortHeight + 60, shortHeight + " vs " + longHeight);
    }

    @Test
    void wrapNeverExceedsTheWidthAndKeepsAllWords() {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            java.awt.FontMetrics fm = g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            String text = "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen";
            List<String> lines = LineChart.wrap(text, fm, 150);
            assertTrue(lines.size() > 1);
            for (String line : lines) {
                assertTrue(fm.stringWidth(line) <= 150, "line too wide: " + line);
            }
            assertEquals(text, String.join(" ", lines));
        } finally {
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ invalid input

    @Test
    void zeroOrNegativeValuesCannotBeOnALogAxis() {
        LineChart chart = new LineChart("T", "x", "y").linearX(1, 2);
        assertThrows(IllegalArgumentException.class,
                () -> chart.add(line("zero", LineChart.BLUE, Marker.CIRCLE, false, 1, 0, 2, 5)));
        assertThrows(IllegalArgumentException.class,
                () -> chart.add(line("negative", LineChart.BLUE, Marker.CIRCLE, false, 1, -1, 2, 5)));
        assertThrows(IllegalArgumentException.class, () -> chart.reference(0, "zero"));
    }

    @Test
    void aSeriesWithoutPointsIsRejected() {
        LineChart chart = new LineChart("T", "x", "y").linearX(1);
        assertThrows(IllegalArgumentException.class, () -> chart.add(new Series("empty", "empty", LineChart.BLUE,
                Marker.CIRCLE, false, List.of(Point.gap()))));
    }

    @Test
    void aLinearAxisNeedsItsTicksAndAChartNeedsASeries() {
        LineChart chart = new LineChart("T", "x", "y");
        assertThrows(IllegalArgumentException.class, chart::linearX);
        assertThrows(IllegalStateException.class, () -> chart.write(folder.resolve("empty.png")));
    }

    @Test
    void aLogXAxisNeedsPositiveX() {
        LineChart chart = new LineChart("T", "x", "y").logX()
                .add(line("a", LineChart.BLUE, Marker.CIRCLE, false, 0, 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> chart.write(folder.resolve("bad.png")));
    }
}
