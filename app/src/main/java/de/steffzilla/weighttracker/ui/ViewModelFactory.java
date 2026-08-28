package de.steffzilla.weighttracker.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import de.steffzilla.weighttracker.AppContainer;
import de.steffzilla.weighttracker.WeightTrackerApplication;

/**
 * Builds every ViewModel in the app from the {@link AppContainer} — one factory rather
 * than one per screen, so a change to what a ViewModel needs has a single place to land.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;

    public ViewModelFactory(AppContainer container) {
        this.container = container;
    }

    /** Convenience for screens: reaches the container through the Application. */
    public static ViewModelFactory from(Context context) {
        Context application = context.getApplicationContext();
        if (!(application instanceof WeightTrackerApplication app)) {
            throw new IllegalStateException(
                    "Expected WeightTrackerApplication, got " + application.getClass().getName());
        }
        return new ViewModelFactory(app.container());
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.equals(WeightViewModel.class)) {
            return (T) new WeightViewModel(
                    container.repository(), container.databaseExecutor(), container.today());
        }
        if (modelClass.equals(StatisticsViewModel.class)) {
            return (T) new StatisticsViewModel(
                    container.repository(), container.calculator(), container.today());
        }
        if (modelClass.equals(BackupViewModel.class)) {
            return (T) new BackupViewModel(
                    container.repository(), container.databaseExecutor());
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
