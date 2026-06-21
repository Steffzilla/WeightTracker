package de.steffzilla.weighttracker.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.stats.ChartModel;
import de.steffzilla.weighttracker.stats.ChartRange;
import de.steffzilla.weighttracker.stats.WeightStatisticsCalculator;

/**
 * Drives the statistics screen: combines the full entry list with the selected
 * {@link ChartRange} and runs the {@link WeightStatisticsCalculator} to expose a
 * ready-to-render {@link ChartModel}. The selected range survives configuration
 * changes because the ViewModel outlives them.
 */
public class StatisticsViewModel extends ViewModel {

    static final ChartRange DEFAULT_RANGE = ChartRange.MONTH;

    private final WeightStatisticsCalculator calculator;
    private final Supplier<LocalDate> today;

    private final LiveData<List<WeightEntry>> entries;
    private final MutableLiveData<ChartRange> selectedRange = new MutableLiveData<>(DEFAULT_RANGE);
    private final MediatorLiveData<ChartModel> chartModel = new MediatorLiveData<>();

    public StatisticsViewModel(WeightRepository repository,
                               WeightStatisticsCalculator calculator,
                               Supplier<LocalDate> today) {
        this.calculator = calculator;
        this.today = today;
        this.entries = repository.getAllEntries();

        chartModel.addSource(entries, e -> recompute());
        chartModel.addSource(selectedRange, r -> recompute());
    }

    public LiveData<ChartModel> getChartModel() {
        return chartModel;
    }

    public LiveData<ChartRange> getSelectedRange() {
        return selectedRange;
    }

    public void setRange(ChartRange range) {
        if (range != selectedRange.getValue()) {
            selectedRange.setValue(range);
        }
    }

    private void recompute() {
        ChartRange range = selectedRange.getValue();
        if (range == null) {
            return;
        }
        List<WeightEntry> current = entries.getValue();
        if (current == null) {
            current = Collections.emptyList();
        }
        chartModel.setValue(calculator.build(current, range, today.get()));
    }
}
