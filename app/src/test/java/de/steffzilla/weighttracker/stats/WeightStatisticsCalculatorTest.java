package de.steffzilla.weighttracker.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.steffzilla.weighttracker.data.WeightEntry;

public class WeightStatisticsCalculatorTest {

    private static final float DELTA = 0.0001f;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 21);

    private WeightStatisticsCalculator calculator;

    @Before
    public void setup() {
        calculator = new WeightStatisticsCalculator();
    }

    private static WeightEntry entry(LocalDate date, float weight) {
        return new WeightEntry(date, weight);
    }

    @Test
    public void emptyInput_returnsEmptyModel() {
        ChartModel model = calculator.build(new ArrayList<>(), ChartRange.ALL, TODAY);

        assertTrue(model.isEmpty());
        assertEquals(0, model.stats().count());
    }

    @Test
    public void rangeWithNoMatchingEntries_returnsEmptyModel() {
        List<WeightEntry> all = List.of(entry(TODAY.minusDays(60), 80f));

        ChartModel model = calculator.build(all, ChartRange.WEEK, TODAY);

        assertTrue(model.isEmpty());
    }

    @Test
    public void singleEntry_statsCollapseAndDegenerateYRange() {
        List<WeightEntry> all = List.of(entry(TODAY, 80f));

        ChartModel model = calculator.build(all, ChartRange.MONTH, TODAY);

        assertEquals(1, model.stats().count());
        assertEquals(80f, model.stats().min(), DELTA);
        assertEquals(80f, model.stats().max(), DELTA);
        assertEquals(80f, model.stats().average(), DELTA);
        assertEquals(0f, model.stats().change(), DELTA);
        // Degenerate span -> ±1 kg padding.
        assertEquals(79f, model.yMin(), DELTA);
        assertEquals(81f, model.yMax(), DELTA);
        assertTrue(model.showMarkers());
    }

    @Test
    public void allEqualWeights_useDegenerateHalfSpan() {
        List<WeightEntry> all = List.of(
                entry(TODAY.minusDays(2), 75f),
                entry(TODAY.minusDays(1), 75f),
                entry(TODAY, 75f));

        ChartModel model = calculator.build(all, ChartRange.MONTH, TODAY);

        assertEquals(74f, model.yMin(), DELTA);
        assertEquals(76f, model.yMax(), DELTA);
        assertEquals(0f, model.stats().change(), DELTA);
    }

    @Test
    public void normalCase_computesStatsAndPaddedYRange() {
        List<WeightEntry> all = List.of(
                entry(TODAY.minusDays(2), 80f),
                entry(TODAY.minusDays(1), 82f),
                entry(TODAY, 78f));

        ChartModel model = calculator.build(all, ChartRange.MONTH, TODAY);

        assertEquals(3, model.stats().count());
        assertEquals(78f, model.stats().min(), DELTA);
        assertEquals(82f, model.stats().max(), DELTA);
        assertEquals(80f, model.stats().average(), DELTA);
        // change = last (78, today) - first (80, today-2)
        assertEquals(-2f, model.stats().change(), DELTA);
        // span 4 -> 10% padding each side
        assertEquals(77.6f, model.yMin(), DELTA);
        assertEquals(82.4f, model.yMax(), DELTA);
    }

    @Test
    public void points_areSortedAscendingByDate() {
        List<WeightEntry> all = new ArrayList<>(Arrays.asList(
                entry(TODAY, 78f),
                entry(TODAY.minusDays(2), 80f),
                entry(TODAY.minusDays(1), 82f)));

        ChartModel model = calculator.build(all, ChartRange.MONTH, TODAY);

        assertEquals(TODAY.minusDays(2), model.points().get(0).date());
        assertEquals(TODAY.minusDays(1), model.points().get(1).date());
        assertEquals(TODAY, model.points().get(2).date());
    }

    @Test
    public void weekRange_includesOldestDayInWindowAndExcludesOlder() {
        // WEEK = 7 days -> window start is today - 6.
        WeightEntry inWindow = entry(TODAY.minusDays(6), 80f);
        WeightEntry justOutside = entry(TODAY.minusDays(7), 90f);
        List<WeightEntry> all = List.of(justOutside, inWindow);

        ChartModel model = calculator.build(all, ChartRange.WEEK, TODAY);

        assertEquals(1, model.stats().count());
        assertEquals(80f, model.points().get(0).weightKg(), DELTA);
    }

    @Test
    public void boundedRange_anchorsXAxisToFirstDataPointAndToday() {
        List<WeightEntry> all = List.of(entry(TODAY.minusDays(3), 80f));

        ChartModel model = calculator.build(all, ChartRange.WEEK, TODAY);

        // X-axis starts at the first datum (not the empty window start) and ends today.
        assertEquals(TODAY.minusDays(3), model.xStart());
        assertEquals(TODAY, model.xEnd());
    }

    @Test
    public void wideWindowWithOnlyRecentData_scalesXAxisToData() {
        // Year window but all entries are from the last few days: the x-axis should
        // span [firstDate .. today], not [today-364 .. today], so the points fill the
        // width instead of overlapping in a strip at the right edge.
        List<WeightEntry> all = List.of(
                entry(TODAY.minusDays(2), 80f),
                entry(TODAY, 79f));

        ChartModel model = calculator.build(all, ChartRange.YEAR, TODAY);

        assertEquals(TODAY.minusDays(2), model.xStart());
        assertEquals(TODAY, model.xEnd());
    }

    @Test
    public void allRange_spansActualDataExtent() {
        List<WeightEntry> all = List.of(
                entry(TODAY.minusDays(400), 90f),
                entry(TODAY.minusDays(5), 80f));

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY);

        assertEquals(2, model.stats().count());
        assertEquals(TODAY.minusDays(400), model.xStart());
        assertEquals(TODAY.minusDays(5), model.xEnd());
    }

    @Test
    public void futureEntries_areIgnored() {
        List<WeightEntry> all = List.of(
                entry(TODAY, 80f),
                entry(TODAY.plusDays(1), 70f));

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY);

        assertEquals(1, model.stats().count());
        assertEquals(80f, model.points().get(0).weightKg(), DELTA);
    }

    @Test
    public void manyEntries_suppressMarkers() {
        List<WeightEntry> all = new ArrayList<>();
        for (int i = 0; i <= WeightStatisticsCalculator.MARKER_THRESHOLD; i++) {
            all.add(entry(TODAY.minusDays(i), 80f + (i % 5)));
        }

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY);

        assertEquals(WeightStatisticsCalculator.MARKER_THRESHOLD + 1, model.stats().count());
        assertFalse(model.showMarkers());
    }

    @Test
    public void exactlyThresholdEntries_keepMarkers() {
        List<WeightEntry> all = new ArrayList<>();
        for (int i = 0; i < WeightStatisticsCalculator.MARKER_THRESHOLD; i++) {
            all.add(entry(TODAY.minusDays(i), 80f + (i % 5)));
        }

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY);

        assertEquals(WeightStatisticsCalculator.MARKER_THRESHOLD, model.stats().count());
        assertTrue(model.showMarkers());
    }

    @Test
    public void fewerPointsThanBudget_areNotAggregated() {
        List<WeightEntry> all = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            all.add(entry(TODAY.minusDays(i), 80f + i));
        }

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY, 10);

        assertEquals(5, model.points().size());
        assertEquals(5, model.stats().count());
        assertTrue(model.showMarkers());
    }

    @Test
    public void morePointsThanBudget_areAggregatedToBucketCount() {
        // 100 daily entries, budget 10 -> 10 equal-width buckets, all non-empty.
        List<WeightEntry> all = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            all.add(entry(TODAY.minusDays(i), 80f + (i % 5)));
        }

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY, 10);

        assertEquals(10, model.points().size());
        // Raw count is preserved in the summary statistics.
        assertEquals(100, model.stats().count());
        // Aggregated points are bucket averages, not real readings -> no markers.
        assertFalse(model.showMarkers());
    }

    @Test
    public void aggregation_averagesWeightsWithinEachBucket() {
        // Four consecutive days, budget 2 -> two buckets of two days each.
        List<WeightEntry> all = List.of(
                entry(TODAY.minusDays(3), 80f),
                entry(TODAY.minusDays(2), 82f),
                entry(TODAY.minusDays(1), 78f),
                entry(TODAY, 76f));

        ChartModel model = calculator.build(all, ChartRange.ALL, TODAY, 2);

        assertEquals(2, model.points().size());
        assertEquals(81f, model.points().get(0).weightKg(), DELTA); // mean(80, 82)
        assertEquals(77f, model.points().get(1).weightKg(), DELTA); // mean(78, 76)
        // Stats stay raw: true extremes and endpoints, not the bucket means.
        assertEquals(76f, model.stats().min(), DELTA);
        assertEquals(82f, model.stats().max(), DELTA);
        assertEquals(79f, model.stats().average(), DELTA);
        assertEquals(-4f, model.stats().change(), DELTA);
        assertEquals(80f, model.stats().firstWeight(), DELTA);
        assertEquals(76f, model.stats().lastWeight(), DELTA);
    }

    @Test
    public void noLimit_keepsEveryPoint() {
        List<WeightEntry> all = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            all.add(entry(TODAY.minusDays(i), 80f + (i % 5)));
        }

        ChartModel model = calculator.build(
                all, ChartRange.ALL, TODAY, WeightStatisticsCalculator.NO_LIMIT);

        assertEquals(50, model.points().size());
    }
}
