package de.steffzilla.weighttracker.stats;

import java.time.LocalDate;

/** A single measured weight value at a date, used as chart input. */
public record ChartPoint(LocalDate date, float weightKg) {
}
