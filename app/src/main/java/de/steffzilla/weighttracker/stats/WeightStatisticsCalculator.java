package de.steffzilla.weighttracker.stats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.steffzilla.weighttracker.data.WeightEntry;

/**
 * Pure, framework-free transformation of raw weight entries into a {@link ChartModel}
 * for a given {@link ChartRange}. {@code today} is injected so the rolling-window
 * filtering is deterministically testable.
 *
 * <p>When a range holds more measurements than the chart can legibly show, the points
 * are down-sampled by averaging into equal-width time buckets (see
 * {@link #build(List, ChartRange, LocalDate, int)}). This smooths a daily up/down
 * "sawtooth" into a readable trend. The summary {@link WeightStatistics} (min, max,
 * average, change) are always computed from the raw data, so true extremes are never
 * hidden by aggregation.
 */
public class WeightStatisticsCalculator {

    /** Above this many drawn points, individual markers are suppressed. */
    public static final int MARKER_THRESHOLD = 40;

    /** Passed as {@code maxPoints} to disable aggregation entirely. */
    public static final int NO_LIMIT = 0;

    /** Fallback point budget used until the view reports its real width. */
    public static final int DEFAULT_MAX_POINTS = 60;

    /** Half-span used for the y-axis when all values are (nearly) equal, in kg. */
    private static final float DEGENERATE_HALF_SPAN = 1.0f;

    /** Fraction of the value span added as padding above and below. */
    private static final float PADDING_FRACTION = 0.1f;

    private static final float SPAN_EPSILON = 0.0001f;

    /** Convenience overload that never aggregates and draws no target band. */
    public ChartModel build(List<WeightEntry> all, ChartRange range, LocalDate today) {
        return build(all, range, today, NO_LIMIT);
    }

    /** Convenience overload without a target band. */
    public ChartModel build(List<WeightEntry> all, ChartRange range, LocalDate today, int maxPoints) {
        return build(all, range, today, maxPoints, WeightBounds.NONE);
    }

    /**
     * @param maxPoints the largest number of points the chart should draw; when the
     *                  range holds more, they are averaged into that many time buckets.
     *                  Use {@link #NO_LIMIT} (or any value {@code <= 0}) to draw every
     *                  raw point.
     * @param bounds    the user's target band; any set side is folded into the y-axis
     *                  range so its reference line is always visible.
     */
    public ChartModel build(List<WeightEntry> all, ChartRange range, LocalDate today,
                            int maxPoints, WeightBounds bounds) {
        LocalDate start = range.isAll() ? null : today.minusDays(range.getDays() - 1L);

        List<WeightEntry> filtered = new ArrayList<>();
        for (WeightEntry e : all) {
            LocalDate d = e.getDate();
            if (d.isAfter(today)) {
                continue;
            }
            if (start != null && d.isBefore(start)) {
                continue;
            }
            filtered.add(e);
        }

        if (filtered.isEmpty()) {
            return ChartModel.EMPTY;
        }

        filtered.sort(Comparator.comparing(WeightEntry::getDate));

        // Summary statistics are computed over the raw measurements, independent of any
        // later down-sampling, so reported extremes stay true.
        List<ChartPoint> rawPoints = new ArrayList<>(filtered.size());
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0f;
        for (WeightEntry e : filtered) {
            float w = e.getWeightKg();
            rawPoints.add(new ChartPoint(e.getDate(), w));
            min = Math.min(min, w);
            max = Math.max(max, w);
            sum += w;
        }

        int count = rawPoints.size();
        float average = sum / count;
        float firstWeight = rawPoints.get(0).weightKg();
        float lastWeight = rawPoints.get(count - 1).weightKg();
        float change = lastWeight - firstWeight;
        LocalDate firstDate = rawPoints.get(0).date();
        LocalDate lastDate = rawPoints.get(count - 1).date();

        // Anchor the x-axis to the actual data extent so points fill the width even
        // when a wide window (e.g. year) holds only recent entries; without this a
        // sparse year crams every point into a tiny strip at the right edge. Bounded
        // ranges still extend the right edge to today so recency stays readable.
        LocalDate xStart = firstDate;
        LocalDate xEnd = range.isAll() ? lastDate : today;

        boolean aggregated = maxPoints > 0 && count > maxPoints;
        List<ChartPoint> points = aggregated
                ? aggregate(rawPoints, maxPoints, xStart, xEnd)
                : rawPoints;

        // Scale the y-axis to the points actually drawn so the line fills the chart.
        float displayMin = Float.MAX_VALUE;
        float displayMax = -Float.MAX_VALUE;
        for (ChartPoint p : points) {
            displayMin = Math.min(displayMin, p.weightKg());
            displayMax = Math.max(displayMax, p.weightKg());
        }

        // Fold any set target bound into the range so its reference line stays visible
        // even when it lies outside the measured values.
        if (bounds.hasLower()) {
            displayMin = Math.min(displayMin, bounds.lowerKg());
            displayMax = Math.max(displayMax, bounds.lowerKg());
        }
        if (bounds.hasUpper()) {
            displayMin = Math.min(displayMin, bounds.upperKg());
            displayMax = Math.max(displayMax, bounds.upperKg());
        }

        float yMin;
        float yMax;
        float span = displayMax - displayMin;
        if (span < SPAN_EPSILON) {
            yMin = displayMin - DEGENERATE_HALF_SPAN;
            yMax = displayMax + DEGENERATE_HALF_SPAN;
        } else {
            float pad = span * PADDING_FRACTION;
            yMin = displayMin - pad;
            yMax = displayMax + pad;
        }

        // Markers mark real measurements; suppress them when the line is dense or its
        // points are bucket averages rather than actual readings.
        boolean showMarkers = !aggregated && points.size() <= MARKER_THRESHOLD;

        WeightStatistics stats = new WeightStatistics(
                count, min, max, average, change, firstWeight, lastWeight, firstDate, lastDate);

        return new ChartModel(points, yMin, yMax, xStart, xEnd, showMarkers, stats,
                bounds.lowerKg(), bounds.upperKg());
    }

    /**
     * Down-samples {@code points} into at most {@code buckets} averaged points by
     * splitting {@code [xStart .. xEnd]} into equal-width day buckets and averaging the
     * date and weight of the measurements that fall in each. Empty buckets are dropped;
     * the result stays sorted ascending by date.
     */
    private static List<ChartPoint> aggregate(List<ChartPoint> points, int buckets,
                                              LocalDate xStart, LocalDate xEnd) {
        long startDay = xStart.toEpochDay();
        long endDay = xEnd.toEpochDay();
        long days = endDay - startDay + 1L;

        double[] weightSum = new double[buckets];
        double[] daySum = new double[buckets];
        int[] counts = new int[buckets];

        for (ChartPoint p : points) {
            long offset = p.date().toEpochDay() - startDay;
            int idx = (int) (offset * buckets / days);
            if (idx < 0) {
                idx = 0;
            } else if (idx >= buckets) {
                idx = buckets - 1;
            }
            weightSum[idx] += p.weightKg();
            daySum[idx] += p.date().toEpochDay();
            counts[idx]++;
        }

        List<ChartPoint> result = new ArrayList<>(buckets);
        for (int b = 0; b < buckets; b++) {
            if (counts[b] == 0) {
                continue;
            }
            long meanDay = Math.round(daySum[b] / counts[b]);
            float meanWeight = (float) (weightSum[b] / counts[b]);
            result.add(new ChartPoint(LocalDate.ofEpochDay(meanDay), meanWeight));
        }
        return result;
    }
}