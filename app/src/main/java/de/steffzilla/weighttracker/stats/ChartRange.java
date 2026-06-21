package de.steffzilla.weighttracker.stats;

import androidx.annotation.StringRes;

import de.steffzilla.weighttracker.R;

/**
 * Selectable time windows for the statistics chart. {@code days} is the rolling
 * window length ending today; {@link #ALL} is unbounded ({@code days == 0}).
 */
public enum ChartRange {

    WEEK(7, R.string.range_week),
    MONTH(30, R.string.range_month),
    THREE_MONTHS(90, R.string.range_three_months),
    YEAR(365, R.string.range_year),
    ALL(0, R.string.range_all);

    private final int days;
    @StringRes
    private final int labelRes;

    ChartRange(int days, @StringRes int labelRes) {
        this.days = days;
        this.labelRes = labelRes;
    }

    public int getDays() {
        return days;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    public boolean isAll() {
        return this == ALL;
    }
}
