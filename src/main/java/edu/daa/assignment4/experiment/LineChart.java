package edu.daa.assignment4.experiment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleFunction;
import javax.imageio.ImageIO;

/**
 * A small line chart drawn with Java2D, so the project needs no charting library and no network access.
 *
 * The y axis is always logarithmic, which is what every plot of this experiment needs (counts that span many
 * orders of magnitude, and ratios). The x axis is linear or logarithmic.
 *
 * Design rules, so that the four plots look like one family:
 * <ul>
 *   <li>The colours are the validated categorical palette (blue, orange, aqua, yellow) in that fixed order. A colour
 *       always means the same input type, in every plot. A series that does not depend on the input is neutral grey.</li>
 *   <li>Colour is never the only code: the algorithm is also told apart by marker shape and line style, every series
 *       is in the legend, and every series is labelled at its right end with its last value.</li>
 *   <li>Lines are 2 px, markers 9 px with a 2 px ring in the background colour, grid lines are 1 px hairlines.</li>
 *   <li>All text is in ink colours, never in a series colour.</li>
 *   <li>The image is rendered at twice the logical size so the text stays sharp when the report scales it.</li>
 * </ul>
 */
final class LineChart {

    enum Marker { CIRCLE, SQUARE, DIAMOND, TRIANGLE }

    /** A data point. A point with NaN coordinates is a gap: the line is broken there. */
    record Point(double x, double y) {
        static Point gap() {
            return new Point(Double.NaN, Double.NaN);
        }

        boolean isGap() {
            return Double.isNaN(x) || Double.isNaN(y);
        }
    }

    /** One line. name is shown in the legend, shortName at the right end of the line. */
    record Series(String name, String shortName, Color color, Marker marker, boolean dashed, List<Point> points) {
    }

    /** A horizontal reference line, for example a ratio of 1. */
    record Reference(double y, String label) {
    }

    // ------------------------------------------------------------------ colours (light mode)

    static final Color BLUE = new Color(0x2a78d6);
    static final Color ORANGE = new Color(0xeb6834);
    static final Color AQUA = new Color(0x1baf7a);
    static final Color YELLOW = new Color(0xeda100);
    /** Secondary ink, used for a series that does not depend on the input type. */
    static final Color NEUTRAL = new Color(0x52514e);

    private static final Color SURFACE = new Color(0xfcfcfb);
    private static final Color INK = new Color(0x0b0b0b);
    private static final Color INK_2 = new Color(0x52514e);
    private static final Color GRID = new Color(0xe1e0d9);
    private static final Color AXIS = new Color(0xc3c2b7);

    // ------------------------------------------------------------------ geometry and fonts (logical pixels)

    private static final int SCALE = 2;
    private static final int WIDTH = 880;
    private static final int PAD = 24;
    /** Width in pixels of every PNG this class writes. */
    static final int IMAGE_WIDTH = WIDTH * SCALE;
    private static final int PLOT_HEIGHT = 330;
    private static final int LEGEND_ROW = 25;
    private static final int LEGEND_SWATCH = 28;
    private static final double MARKER_RADIUS = 4.5;
    private static final double LABEL_GAP = 18;
    private static final double FOOTNOTE_LINE = 16;
    private static final double CAPTION_LINE = 22;

    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 19);
    private static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font TICK_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font CAPTION_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    private static final Font NOTE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    // ------------------------------------------------------------------ content

    private final String title;
    private final String xTitle;
    private final String yTitle;
    private String caption = "";
    private String footnote = "";
    private boolean logX;
    private double[] xTicks;
    private DoubleFunction<String> xFormat = v -> Fmt.tick(v, false);
    private DoubleFunction<String> yFormat = v -> Fmt.tick(v, true);
    private DoubleFunction<String> valueFormat = Fmt::compact;
    private final List<Series> series = new ArrayList<>();
    private final List<Reference> references = new ArrayList<>();

    LineChart(String title, String xTitle, String yTitle) {
        this.title = Objects.requireNonNull(title);
        this.xTitle = Objects.requireNonNull(xTitle);
        this.yTitle = Objects.requireNonNull(yTitle);
    }

    String title() {
        return title;
    }

    String caption() {
        return caption;
    }

    /** The one-sentence takeaway printed under the plot. */
    LineChart caption(String text) {
        this.caption = Objects.requireNonNull(text);
        return this;
    }

    /** A small line at the bottom, used to say where the numbers come from. */
    LineChart footnote(String text) {
        this.footnote = Objects.requireNonNull(text);
        return this;
    }

    /** Linear x axis with ticks at the given values. */
    LineChart linearX(double... ticks) {
        if (ticks.length == 0) {
            throw new IllegalArgumentException("A linear x axis needs its tick values.");
        }
        this.logX = false;
        this.xTicks = ticks.clone();
        return this;
    }

    /** Logarithmic x axis. With no tick values given, ticks are chosen automatically (1, 2, 5, 10, ...). */
    LineChart logX(double... ticks) {
        this.logX = true;
        this.xTicks = ticks.length == 0 ? null : ticks.clone();
        return this;
    }

    LineChart xTickFormat(DoubleFunction<String> format) {
        this.xFormat = Objects.requireNonNull(format);
        return this;
    }

    LineChart yTickFormat(DoubleFunction<String> format) {
        this.yFormat = Objects.requireNonNull(format);
        return this;
    }

    /** Format of the value printed after each series name at the right end of the line. */
    LineChart valueFormat(DoubleFunction<String> format) {
        this.valueFormat = Objects.requireNonNull(format);
        return this;
    }

    LineChart add(Series s) {
        boolean anyPoint = false;
        for (Point p : s.points()) {
            if (p.isGap()) {
                continue;
            }
            anyPoint = true;
            if (!(p.y() > 0)) {
                throw new IllegalArgumentException("The y axis is logarithmic, so every y must be positive: " + s.name());
            }
        }
        if (!anyPoint) {
            throw new IllegalArgumentException("Series has no points: " + s.name());
        }
        series.add(s);
        return this;
    }

    LineChart reference(double y, String label) {
        if (!(y > 0)) {
            throw new IllegalArgumentException("A reference line on a log axis needs a positive value.");
        }
        references.add(new Reference(y, label));
        return this;
    }

    /** Draws the chart and writes it as a PNG, creating the folder if needed. */
    void write(Path file) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (series.isEmpty()) {
            throw new IllegalStateException("Nothing to draw: the chart has no series.");
        }

        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D measure = scratch.createGraphics();
        Layout layout;
        try {
            prepare(measure);
            layout = layout(measure);
        } finally {
            measure.dispose();
        }

        BufferedImage image = new BufferedImage(WIDTH * SCALE, layout.height * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            prepare(g);
            paint(g, layout);
        } finally {
            g.dispose();
        }

        Path folder = file.toAbsolutePath().getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        if (!ImageIO.write(image, "png", file.toFile())) {
            throw new IOException("No PNG writer is available in this Java installation.");
        }
    }

    private static void prepare(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.scale(SCALE, SCALE);
    }

    // ------------------------------------------------------------------ axes

    private static final class Axis {
        final boolean log;
        final double min;
        final double max;
        final List<Double> ticks;

        Axis(boolean log, double min, double max, List<Double> ticks) {
            this.log = log;
            this.min = min;
            this.max = max;
            this.ticks = ticks;
        }

        /** Position of a value along the axis, 0 at min and 1 at max. */
        double fraction(double v) {
            if (log) {
                return (Math.log(v) - Math.log(min)) / (Math.log(max) - Math.log(min));
            }
            return (v - min) / (max - min);
        }
    }

    private Axis xAxis() {
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (Series s : series) {
            for (Point p : s.points()) {
                if (!p.isGap()) {
                    lo = Math.min(lo, p.x());
                    hi = Math.max(hi, p.x());
                }
            }
        }
        if (logX) {
            if (!(lo > 0)) {
                throw new IllegalArgumentException("A logarithmic x axis needs positive x values.");
            }
            double min = lo / 1.1;
            double max = hi * 1.1;
            return new Axis(true, min, max, xTicks == null ? logTicks(min, max) : inside(xTicks, min, max));
        }
        double pad = (hi > lo ? hi - lo : 1) * 0.05;
        return new Axis(false, lo - pad, hi + pad, inside(xTicks, lo - pad, hi + pad));
    }

    private Axis yAxis() {
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (Series s : series) {
            for (Point p : s.points()) {
                if (!p.isGap()) {
                    lo = Math.min(lo, p.y());
                    hi = Math.max(hi, p.y());
                }
            }
        }
        for (Reference r : references) {
            lo = Math.min(lo, r.y());
            hi = Math.max(hi, r.y());
        }
        // Snap the ends to 1, 2 or 5 times a power of ten so the first and last grid lines sit on round numbers.
        double min = snap125(lo / 1.1, false);
        double max = snap125(hi * 1.1, true);
        return new Axis(true, min, max, logTicks(min, max));
    }

    /** The nearest value of the form 1, 2 or 5 times a power of ten below (or above) v. */
    static double snap125(double v, boolean up) {
        double base = Math.pow(10, Math.floor(Math.log10(v)));
        double[] steps = {1, 2, 5, 10};
        if (up) {
            for (double step : steps) {
                if (step * base >= v * (1 - 1e-12)) {
                    return step * base;
                }
            }
        } else {
            for (int i = steps.length - 1; i >= 0; i--) {
                if (steps[i] * base <= v * (1 + 1e-12)) {
                    return steps[i] * base;
                }
            }
        }
        throw new IllegalStateException("unreachable: " + v);
    }

    /** Ticks for a log axis: powers of ten when there are at least four of them, otherwise 1, 2 and 5 times those. */
    static List<Double> logTicks(double min, double max) {
        List<Double> decades = new ArrayList<>();
        List<Double> all = new ArrayList<>();
        int first = (int) Math.floor(Math.log10(min));
        int last = (int) Math.ceil(Math.log10(max));
        for (int k = first; k <= last; k++) {
            for (double step : new double[]{1, 2, 5}) {
                double v = step * Math.pow(10, k);
                if (v >= min * (1 - 1e-9) && v <= max * (1 + 1e-9)) {
                    all.add(v);
                    if (step == 1) {
                        decades.add(v);
                    }
                }
            }
        }
        return decades.size() >= 4 ? decades : all;
    }

    private static List<Double> inside(double[] ticks, double min, double max) {
        List<Double> kept = new ArrayList<>();
        for (double t : ticks) {
            if (t >= min && t <= max) {
                kept.add(t);
            }
        }
        return kept;
    }

    // ------------------------------------------------------------------ layout

    private static final class Layout {
        Axis x;
        Axis y;
        double plotLeft;
        double plotRight;
        double plotTop;
        double plotBottom;
        double titleBaseline;
        double legendTop;
        List<double[]> legendSlots = new ArrayList<>();
        double xTickBaseline;
        double xTitleBaseline;
        List<String> captionLines = new ArrayList<>();
        double captionBaseline;
        List<String> footnoteLines = new ArrayList<>();
        double footnoteBaseline;
        // One label per series and per reference line, in a column right of the plot.
        String[] labelName;
        String[] labelValue;
        double[] labelAnchorX;
        double[] labelAnchorY;
        double[] labelY;
        int height;

        double xPos(double v) {
            return plotLeft + x.fraction(v) * (plotRight - plotLeft);
        }

        double yPos(double v) {
            return plotBottom - y.fraction(v) * (plotBottom - plotTop);
        }
    }

    private Layout layout(Graphics2D m) {
        Layout l = new Layout();
        l.x = xAxis();
        l.y = yAxis();

        FontMetrics tick = m.getFontMetrics(TICK_FONT);
        FontMetrics text = m.getFontMetrics(TEXT_FONT);
        FontMetrics bold = m.getFontMetrics(VALUE_FONT);
        FontMetrics captionFm = m.getFontMetrics(CAPTION_FONT);

        double tickWidth = 0;
        for (double t : l.y.ticks) {
            tickWidth = Math.max(tickWidth, tick.stringWidth(yFormat.apply(t)));
        }

        // The label of each line (name and last value) and of each reference line lives in a column right of the
        // plot, so it never covers data.
        int labels = series.size() + references.size();
        l.labelName = new String[labels];
        l.labelValue = new String[labels];
        double labelWidth = 0;
        for (int i = 0; i < series.size(); i++) {
            Series s = series.get(i);
            l.labelName[i] = s.shortName() + "  ";
            l.labelValue[i] = valueFormat.apply(lastPoint(s).y());
            labelWidth = Math.max(labelWidth, text.stringWidth(l.labelName[i]) + bold.stringWidth(l.labelValue[i]));
        }
        for (int j = 0; j < references.size(); j++) {
            int i = series.size() + j;
            l.labelName[i] = references.get(j).label();
            l.labelValue[i] = "";
            labelWidth = Math.max(labelWidth, text.stringWidth(l.labelName[i]));
        }

        l.plotLeft = PAD + text.getHeight() + 8 + tickWidth + 8;
        l.plotRight = WIDTH - PAD - labelWidth - 14;

        l.titleBaseline = PAD + 16;
        l.legendTop = l.titleBaseline + 14;
        double legendBottom = l.legendTop;
        if (series.size() >= 2) {
            int row = 0;
            double x = PAD;
            for (Series s : series) {
                double w = LEGEND_SWATCH + 8 + text.stringWidth(s.name());
                if (x > PAD && x + w > WIDTH - PAD) {
                    row++;
                    x = PAD;
                }
                l.legendSlots.add(new double[]{x, row});
                x += w + 24;
            }
            legendBottom = l.legendTop + (row + 1) * LEGEND_ROW;
        }

        l.plotTop = legendBottom + 18;
        l.plotBottom = l.plotTop + PLOT_HEIGHT;
        l.xTickBaseline = l.plotBottom + 22;
        l.xTitleBaseline = l.plotBottom + 50;

        l.labelAnchorX = new double[labels];
        l.labelAnchorY = new double[labels];
        for (int i = 0; i < series.size(); i++) {
            Point last = lastPoint(series.get(i));
            l.labelAnchorX[i] = l.xPos(last.x()) + MARKER_RADIUS + 3;
            l.labelAnchorY[i] = l.yPos(last.y());
        }
        for (int j = 0; j < references.size(); j++) {
            l.labelAnchorX[series.size() + j] = l.plotRight + 2;
            l.labelAnchorY[series.size() + j] = l.yPos(references.get(j).y());
        }
        l.labelY = spread(l.labelAnchorY, LABEL_GAP, l.plotTop + 8, l.plotBottom - 8);

        double next = l.xTitleBaseline + 38;
        if (!caption.isBlank()) {
            l.captionLines = wrap(caption, captionFm, WIDTH - 2.0 * PAD);
            l.captionBaseline = next;
            next += (l.captionLines.size() - 1) * CAPTION_LINE + 30;
        }
        l.footnoteBaseline = next;
        if (!footnote.isBlank()) {
            l.footnoteLines = wrap(footnote, m.getFontMetrics(NOTE_FONT), WIDTH - 2.0 * PAD);
            next += (l.footnoteLines.size() - 1) * FOOTNOTE_LINE;
        }
        l.height = (int) Math.ceil(next + 20);
        return l;
    }

    private static Point lastPoint(Series s) {
        for (int i = s.points().size() - 1; i >= 0; i--) {
            if (!s.points().get(i).isGap()) {
                return s.points().get(i);
            }
        }
        throw new IllegalStateException("Series has no points: " + s.name());
    }

    static List<String> wrap(String text, FontMetrics fm, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (line.length() > 0 && fm.stringWidth(line + " " + word) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Moves labels apart so that neighbours are at least gap apart, while keeping each label as close as possible
     * to the position it wants (the y of its line end). Labels that already have room stay where they are.
     * The result is in the same order as the input and lies inside [lo, hi] when the labels fit there.
     */
    static double[] spread(double[] desired, double gap, double lo, double hi) {
        int n = desired.length;
        if (n == 0) {
            return new double[0];
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(i -> desired[i]));

        // A cluster is a run of neighbours (in sorted order) that sit exactly gap apart. It stores its size and
        // the sum of (wanted position - index inside the cluster * gap); dividing that by the size gives the top
        // position that keeps the cluster's members as close as possible to what they want.
        List<double[]> clusters = new ArrayList<>();
        for (int index : order) {
            clusters.add(new double[]{1, desired[index]});
            while (clusters.size() > 1) {
                double[] b = clusters.get(clusters.size() - 1);
                double[] a = clusters.get(clusters.size() - 2);
                if (a[1] / a[0] + a[0] * gap <= b[1] / b[0]) {
                    break;
                }
                a[1] += b[1] - b[0] * a[0] * gap;
                a[0] += b[0];
                clusters.remove(clusters.size() - 1);
            }
        }

        double[] sorted = new double[n];
        int k = 0;
        for (double[] cluster : clusters) {
            double top = cluster[1] / cluster[0];
            for (int j = 0; j < (int) cluster[0]; j++) {
                sorted[k++] = top + j * gap;
            }
        }

        double shift = 0;
        if (sorted[0] < lo) {
            shift = lo - sorted[0];
        } else if (sorted[n - 1] > hi) {
            shift = Math.max(hi - sorted[n - 1], lo - sorted[0]);
        }
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[order[i]] = sorted[i] + shift;
        }
        return result;
    }

    // ------------------------------------------------------------------ painting

    private void paint(Graphics2D g, Layout l) {
        g.setColor(SURFACE);
        g.fillRect(0, 0, WIDTH, l.height);

        g.setFont(TITLE_FONT);
        g.setColor(INK);
        g.drawString(title, PAD, (float) l.titleBaseline);

        paintLegend(g, l);
        paintGridAndAxes(g, l);
        paintReferences(g, l);
        paintSeries(g, l);
        paintEndLabels(g, l);

        g.setFont(CAPTION_FONT);
        g.setColor(INK);
        for (int i = 0; i < l.captionLines.size(); i++) {
            g.drawString(l.captionLines.get(i), PAD, (float) (l.captionBaseline + i * CAPTION_LINE));
        }
        g.setFont(NOTE_FONT);
        g.setColor(INK_2);
        for (int i = 0; i < l.footnoteLines.size(); i++) {
            g.drawString(l.footnoteLines.get(i), PAD, (float) (l.footnoteBaseline + i * FOOTNOTE_LINE));
        }
    }

    private void paintLegend(Graphics2D g, Layout l) {
        if (series.size() < 2) {
            return;
        }
        g.setFont(TEXT_FONT);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < series.size(); i++) {
            Series s = series.get(i);
            double x = l.legendSlots.get(i)[0];
            double cy = l.legendTop + l.legendSlots.get(i)[1] * LEGEND_ROW + LEGEND_ROW / 2.0;
            g.setColor(s.color());
            g.setStroke(lineStroke(s.dashed()));
            g.draw(new Line2D.Double(x, cy, x + LEGEND_SWATCH, cy));
            drawMarker(g, s, x + LEGEND_SWATCH / 2.0, cy);
            g.setColor(INK_2);
            g.setFont(TEXT_FONT);
            g.drawString(s.name(), (float) (x + LEGEND_SWATCH + 8), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
        }
    }

    private void paintGridAndAxes(Graphics2D g, Layout l) {
        g.setStroke(new BasicStroke(1f));
        g.setFont(TICK_FONT);
        FontMetrics tick = g.getFontMetrics();
        double half = (tick.getAscent() - tick.getDescent()) / 2.0;

        for (double t : l.y.ticks) {
            double py = l.yPos(t);
            g.setColor(GRID);
            g.draw(new Line2D.Double(l.plotLeft, py, l.plotRight, py));
            String label = yFormat.apply(t);
            g.setColor(INK_2);
            g.drawString(label, (float) (l.plotLeft - 8 - tick.stringWidth(label)), (float) (py + half));
        }

        g.setColor(AXIS);
        g.draw(new Line2D.Double(l.plotLeft, l.plotBottom, l.plotRight, l.plotBottom));
        for (double t : l.x.ticks) {
            double px = l.xPos(t);
            g.setColor(AXIS);
            g.draw(new Line2D.Double(px, l.plotBottom, px, l.plotBottom + 4));
            String label = xFormat.apply(t);
            g.setColor(INK_2);
            g.drawString(label, (float) (px - tick.stringWidth(label) / 2.0), (float) l.xTickBaseline);
        }

        g.setFont(TEXT_FONT);
        g.setColor(INK_2);
        FontMetrics fm = g.getFontMetrics();
        double cx = (l.plotLeft + l.plotRight) / 2.0;
        g.drawString(xTitle, (float) (cx - fm.stringWidth(xTitle) / 2.0), (float) l.xTitleBaseline);

        AffineTransform saved = g.getTransform();
        double px = PAD + fm.getAscent() - 2;
        double py = (l.plotTop + l.plotBottom) / 2.0;
        g.rotate(-Math.PI / 2, px, py);
        g.drawString(yTitle, (float) (px - fm.stringWidth(yTitle) / 2.0), (float) py);
        g.setTransform(saved);
    }

    private void paintReferences(Graphics2D g, Layout l) {
        for (Reference r : references) {
            double py = l.yPos(r.y());
            g.setColor(INK_2);
            g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{5f, 4f}, 0f));
            g.draw(new Line2D.Double(l.plotLeft, py, l.plotRight, py));
        }
    }

    private void paintSeries(Graphics2D g, Layout l) {
        for (Series s : series) {
            g.setColor(s.color());
            g.setStroke(lineStroke(s.dashed()));
            Path2D.Double path = null;
            for (Point p : s.points()) {
                if (p.isGap()) {
                    if (path != null) {
                        g.draw(path);
                    }
                    path = null;
                    continue;
                }
                double px = l.xPos(p.x());
                double py = l.yPos(p.y());
                if (path == null) {
                    path = new Path2D.Double();
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            if (path != null) {
                g.draw(path);
            }
        }
        for (Series s : series) {
            for (Point p : s.points()) {
                if (!p.isGap()) {
                    drawMarker(g, s, l.xPos(p.x()), l.yPos(p.y()));
                }
            }
        }
    }

    private void paintEndLabels(Graphics2D g, Layout l) {
        double labelX = l.plotRight + 12;
        for (int i = 0; i < l.labelName.length; i++) {
            double ax = l.labelAnchorX[i];
            double ay = l.labelAnchorY[i];
            double ly = l.labelY[i];

            // A thin leader joins the end of the line to its label when the label moved or is far away.
            if (Math.abs(ly - ay) > 1.5 || labelX - 4 - ax > 8) {
                g.setColor(AXIS);
                g.setStroke(new BasicStroke(1f));
                g.draw(new Line2D.Double(ax, ay, labelX - 4, ly));
            }

            g.setFont(TEXT_FONT);
            FontMetrics fm = g.getFontMetrics();
            float baseline = (float) (ly + (fm.getAscent() - fm.getDescent()) / 2.0);
            g.setColor(INK_2);
            g.drawString(l.labelName[i], (float) labelX, baseline);
            if (!l.labelValue[i].isEmpty()) {
                g.setFont(VALUE_FONT);
                g.setColor(INK);
                g.drawString(l.labelValue[i], (float) (labelX + fm.stringWidth(l.labelName[i])), baseline);
            }
        }
    }

    private static BasicStroke lineStroke(boolean dashed) {
        if (dashed) {
            return new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 5f}, 0f);
        }
        return new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static void drawMarker(Graphics2D g, Series s, double cx, double cy) {
        Shape shape = markerShape(s.marker(), cx, cy, MARKER_RADIUS);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(SURFACE);
        g.draw(shape);
        g.setColor(s.color());
        g.fill(shape);
    }

    private static Shape markerShape(Marker marker, double cx, double cy, double r) {
        switch (marker) {
            case SQUARE:
                return new Rectangle2D.Double(cx - r * 0.9, cy - r * 0.9, r * 1.8, r * 1.8);
            case DIAMOND: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(cx, cy - r * 1.3);
                p.lineTo(cx + r * 1.3, cy);
                p.lineTo(cx, cy + r * 1.3);
                p.lineTo(cx - r * 1.3, cy);
                p.closePath();
                return p;
            }
            case TRIANGLE: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(cx, cy - r * 1.25);
                p.lineTo(cx + r * 1.15, cy + r * 0.9);
                p.lineTo(cx - r * 1.15, cy + r * 0.9);
                p.closePath();
                return p;
            }
            case CIRCLE:
            default:
                return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        }
    }
}
