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
 */
public class WeightStatisticsCalculator {

    /** Above this many points in range, individual markers are suppressed. */
    public static final int MARKER_THRESHOLD = 40;

    /** Half-span used for the y-axis when all values are (nearly) equal, in kg. */
    private static final float DEGENERATE_HALF_SPAN = 1.0f;

    /** Fraction of the value span added as padding above and below. */
    private static final float PADDING_FRACTION = 0.1f;

    private static final float SPAN_EPSILON = 0.0001f;

    public ChartModel build(List<WeightEntry> all, ChartRange range, LocalDate today) {
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

        List<ChartPoint> points = new ArrayList<>(filtered.size());
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0f;
        for (WeightEntry e : filtered) {
            float w = e.getWeightKg();
            points.add(new ChartPoint(e.getDate(), w));
            min = Math.min(min, w);
            max = Math.max(max, w);
            sum += w;
        }

        int count = points.size();
        float average = sum / count;
        float firstWeight = points.get(0).weightKg();
        float lastWeight = points.get(count - 1).weightKg();
        float change = lastWeight - firstWeight;
        LocalDate firstDate = points.get(0).date();
        LocalDate lastDate = points.get(count - 1).date();

        float yMin;
        float yMax;
        float span = max - min;
        if (span < SPAN_EPSILON) {
            yMin = min - DEGENERATE_HALF_SPAN;
            yMax = max + DEGENERATE_HALF_SPAN;
        } else {
            float pad = span * PADDING_FRACTION;
            yMin = min - pad;
            yMax = max + pad;
        }

        // Bounded ranges anchor the x-axis to [start .. today] so the most recent
        // value stays at the right edge; ALL spans the actual data extent.
        LocalDate xStart = range.isAll() ? firstDate : start;
        LocalDate xEnd = range.isAll() ? lastDate : today;

        boolean showMarkers = count <= MARKER_THRESHOLD;

        WeightStatistics stats = new WeightStatistics(
                count, min, max, average, change, firstDate, lastDate);

        return new ChartModel(points, yMin, yMax, xStart, xEnd, showMarkers, stats);
    }
}
