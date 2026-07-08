package de.steffzilla.weighttracker.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executors;

import de.steffzilla.weighttracker.data.AppDatabase;
import de.steffzilla.weighttracker.data.WeightRepository;

public class BackupViewModelFactory implements ViewModelProvider.Factory {

    private final WeightRepository repository;

    public BackupViewModelFactory(Context context) {
        var db = AppDatabase.getInstance(context);
        this.repository = new WeightRepository(db.weightDao());
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(BackupViewModel.class)) {
            return (T) new BackupViewModel(repository, Executors.newSingleThreadExecutor());
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}