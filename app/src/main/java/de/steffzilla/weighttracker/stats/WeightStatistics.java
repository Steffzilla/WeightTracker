package de.steffzilla.weighttracker.stats;

import java.time.LocalDate;

/**
 * Aggregate figures for the entries within a selected range. {@code change} is the
 * net difference of the last value minus the first value in the range. For an empty
 * range use {@link #EMPTY}.
 */
public record WeightStatistics(
        int count,
        float min,
        float max,
        float average,
        float change,
        LocalDate firstDate,
        LocalDate lastDate) {

    public static final WeightStatistics EMPTY =
            new WeightStatistics(0, 0f, 0f, 0f, 0f, null, null);
}
