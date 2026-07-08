package de.steffzilla.weighttracker.backup;

import java.time.LocalDate;
import java.util.List;

/**
 * The result of reconciling parsed CSV rows against the existing data set:
 * <ul>
 *   <li>{@code collisions} — dates present with a different weight (block the import),</li>
 *   <li>{@code toInsert} — genuinely new rows to write,</li>
 *   <li>{@code identicalSkipped} — rows whose date and weight already exist (no-ops).</li>
 * </ul>
 */
public record ImportPlan(List<LocalDate> collisions, List<ParsedEntry> toInsert, int identicalSkipped) {

    public boolean hasCollisions() {
        return !collisions.isEmpty();
    }
}