package de.steffzilla.weighttracker.stats;

import java.time.LocalDate;
import java.util.List;

/**
 * Fully prepared render input for the chart view. {@code yMin}/{@code yMax} already
 * include padding (never zero-based); {@code xStart}/{@code xEnd} are the date bounds
 * the points are mapped against; {@code showMarkers} signals whether individual point
 * dots should be drawn (suppressed when there are many points).
 */
public record ChartModel(
        List<ChartPoint> points,
        float yMin,
        float yMax,
        LocalDate xStart,
        LocalDate xEnd,
        boolean showMarkers,
        WeightStatistics stats) {

    public static final ChartModel EMPTY =
            new ChartModel(List.of(), 0f, 0f, null, null, false, WeightStatistics.EMPTY);

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
