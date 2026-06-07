package de.steffzilla.weighttracker.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executors;

import de.steffzilla.weighttracker.data.AppDatabase;
import de.steffzilla.weighttracker.data.WeightRepository;

public class WeightViewModelFactory implements ViewModelProvider.Factory {

    private final WeightRepository repository;

    public WeightViewModelFactory(Context context) {
        var db = AppDatabase.getInstance(context);
        this.repository = new WeightRepository(db.weightDao());
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WeightViewModel.class)) {
            return (T) new WeightViewModel(repository, Executors.newSingleThreadExecutor());
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}