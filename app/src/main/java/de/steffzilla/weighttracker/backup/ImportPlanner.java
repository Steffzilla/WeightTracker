package de.steffzilla.weighttracker.backup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.steffzilla.weighttracker.data.WeightEntry;

/**
 * Pure decision logic for merging parsed CSV rows into the existing data set. A row whose
 * date already exists with the <em>same</em> weight is a no-op (skipped); a date that
 * exists with a <em>different</em> weight is a collision that blocks the whole import; any
 * other date is new and will be inserted.
 *
 * <p>Weights are compared rounded to {@link #COMPARISON_DECIMALS} decimals so that float
 * representation noise never produces a spurious collision, while genuinely different
 * values (finer than anyone would ever enter) are still distinguished.
 */
public final class ImportPlanner {

    /** Decimal places used when comparing weights for equality. */
    public static final int COMPARISON_DECIMALS = 3;

    private static final float SCALE = 1000f; // 10^COMPARISON_DECIMALS

    private ImportPlanner() {
    }

    public static ImportPlan plan(List<WeightEntry> existing, List<ParsedEntry> incoming) {
        Map<LocalDate, Integer> existingByDate = new HashMap<>();
        for (WeightEntry entry : existing) {
            existingByDate.put(entry.getDate(), rounded(entry.getWeightKg()));
        }

        List<LocalDate> collisions = new ArrayList<>();
        List<ParsedEntry> toInsert = new ArrayList<>();
        int identicalSkipped = 0;

        for (ParsedEntry incomingEntry : incoming) {
            Integer existingWeight = existingByDate.get(incomingEntry.date());
            if (existingWeight == null) {
                toInsert.add(incomingEntry);
            } else if (existingWeight == rounded(incomingEntry.weightKg())) {
                identicalSkipped++;
            } else {
                collisions.add(incomingEntry.date());
            }
        }
        return new ImportPlan(collisions, toInsert, identicalSkipped);
    }

    private static int rounded(float weightKg) {
        return Math.round(weightKg * SCALE);
    }
}