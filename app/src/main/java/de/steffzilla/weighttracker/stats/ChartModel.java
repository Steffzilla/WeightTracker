package de.steffzilla.weighttracker.stats;

import java.time.LocalDate;
import java.util.List;

/**
 * Fully prepared render input for the chart view. {@code yMin}/{@code yMax} already
 * include padding (never zero-based) and any set target bound; {@code xStart}/{@code
 * xEnd} are the date bounds the points are mapped against; {@code showMarkers} signals
 * whether individual point dots should be drawn (suppressed when there are many points).
 * {@code lowerBound}/{@code upperBound} are the user's optional target band in kg, each
 * {@code null} when unset, drawn as horizontal reference lines.
 */
public record ChartModel(
        List<ChartPoint> points,
        float yMin,
        float yMax,
        LocalDate xStart,
        LocalDate xEnd,
        boolean showMarkers,
        WeightStatistics stats,
        Float lowerBound,
        Float upperBound) {

    public static final ChartModel EMPTY =
            new ChartModel(List.of(), 0f, 0f, null, null, false, WeightStatistics.EMPTY, null, null);

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
