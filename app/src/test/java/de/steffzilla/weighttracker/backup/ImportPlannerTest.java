package de.steffzilla.weighttracker.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.steffzilla.weighttracker.data.WeightEntry;

public class ImportPlannerTest {

    private static final LocalDate D1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 1, 2);

    @Test
    public void plan_newDatesAreQueuedForInsert() {
        ImportPlan plan = ImportPlanner.plan(
                Collections.emptyList(),
                Arrays.asList(new ParsedEntry(D1, 80.0f), new ParsedEntry(D2, 79.0f)));

        assertFalse(plan.hasCollisions());
        assertEquals(2, plan.toInsert().size());
        assertEquals(0, plan.identicalSkipped());
    }

    @Test
    public void plan_sameDateSameValueIsSkippedNotCollision() {
        List<WeightEntry> existing = List.of(new WeightEntry(D1, 80.0f));

        ImportPlan plan = ImportPlanner.plan(existing, List.of(new ParsedEntry(D1, 80.0f)));

        assertFalse(plan.hasCollisions());
        assertTrue(plan.toInsert().isEmpty());
        assertEquals(1, plan.identicalSkipped());
    }

    @Test
    public void plan_sameDateDifferentValueIsCollision() {
        List<WeightEntry> existing = List.of(new WeightEntry(D1, 80.0f));

        ImportPlan plan = ImportPlanner.plan(existing, List.of(new ParsedEntry(D1, 81.0f)));

        assertTrue(plan.hasCollisions());
        assertEquals(List.of(D1), plan.collisions());
        assertTrue(plan.toInsert().isEmpty());
    }

    @Test
    public void plan_tinyFloatNoiseWithinToleranceIsNotACollision() {
        List<WeightEntry> existing = List.of(new WeightEntry(D1, 80.0f));

        // 80.0001 rounds to the same 3-decimal value as 80.0 -> treated as identical
        ImportPlan plan = ImportPlanner.plan(existing, List.of(new ParsedEntry(D1, 80.0001f)));

        assertFalse(plan.hasCollisions());
        assertEquals(1, plan.identicalSkipped());
    }

    @Test
    public void plan_mixesInsertsSkipsAndCollisions() {
        List<WeightEntry> existing = Arrays.asList(
                new WeightEntry(D1, 80.0f),
                new WeightEntry(D2, 79.0f));

        ImportPlan plan = ImportPlanner.plan(existing, Arrays.asList(
                new ParsedEntry(D1, 80.0f),                     // identical -> skip
                new ParsedEntry(D2, 78.0f),                     // conflict
                new ParsedEntry(LocalDate.of(2026, 1, 3), 77.5f))); // new -> insert

        assertTrue(plan.hasCollisions());
        assertEquals(List.of(D2), plan.collisions());
        assertEquals(1, plan.toInsert().size());
        assertEquals(1, plan.identicalSkipped());
    }
}