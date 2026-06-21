package de.steffzilla.weighttracker.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.stats.ChartModel;
import de.steffzilla.weighttracker.stats.ChartRange;
import de.steffzilla.weighttracker.stats.WeightStatisticsCalculator;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 21);

    @Mock
    WeightRepository repository;

    private MutableLiveData<List<WeightEntry>> entriesLiveData;
    private StatisticsViewModel viewModel;

    @Before
    public void setup() {
        entriesLiveData = new MutableLiveData<>();
        when(repository.getAllEntries()).thenReturn(entriesLiveData);
        viewModel = new StatisticsViewModel(
                repository, new WeightStatisticsCalculator(), () -> TODAY);
        // MediatorLiveData only observes its sources while it has active observers.
        viewModel.getChartModel().observeForever(model -> { });
    }

    @Test
    public void defaultRange_isMonth() {
        assertEquals(ChartRange.MONTH, viewModel.getSelectedRange().getValue());
    }

    @Test
    public void entriesWithinRange_produceModel() {
        entriesLiveData.setValue(List.of(new WeightEntry(TODAY, 80f)));

        ChartModel model = viewModel.getChartModel().getValue();
        assertEquals(1, model.stats().count());
    }

    @Test
    public void emptyEntries_produceEmptyModel() {
        entriesLiveData.setValue(Collections.emptyList());

        assertTrue(viewModel.getChartModel().getValue().isEmpty());
    }

    @Test
    public void setRange_reflectsInSelectedRange() {
        viewModel.setRange(ChartRange.YEAR);

        assertEquals(ChartRange.YEAR, viewModel.getSelectedRange().getValue());
    }

    @Test
    public void setRange_reappliesFilterToExistingEntries() {
        entriesLiveData.setValue(List.of(
                new WeightEntry(TODAY.minusDays(20), 80f),
                new WeightEntry(TODAY, 79f)));

        // MONTH (30 days): both entries are inside the window.
        assertEquals(2, viewModel.getChartModel().getValue().stats().count());

        viewModel.setRange(ChartRange.WEEK);

        // WEEK (7 days): the 20-day-old entry drops out.
        assertEquals(1, viewModel.getChartModel().getValue().stats().count());
    }
}
