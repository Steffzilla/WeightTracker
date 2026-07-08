package de.steffzilla.weighttracker.backup;

import java.time.LocalDate;

/** A single weight measurement parsed from a CSV row, before it is merged into the store. */
public record ParsedEntry(LocalDate date, float weightKg) {
}