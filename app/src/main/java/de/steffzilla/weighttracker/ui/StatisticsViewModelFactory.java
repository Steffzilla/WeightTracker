package de.steffzilla.weighttracker.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;

import de.steffzilla.weighttracker.data.AppDatabase;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.stats.WeightStatisticsCalculator;

public class StatisticsViewModelFactory implements ViewModelProvider.Factory {

    private final WeightRepository repository;

    public StatisticsViewModelFactory(Context context) {
        var db = AppDatabase.getInstance(context);
        this.repository = new WeightRepository(db.weightDao());
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StatisticsViewModel.class)) {
            return (T) new StatisticsViewModel(
                    repository, new WeightStatisticsCalculator(), LocalDate::now);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
