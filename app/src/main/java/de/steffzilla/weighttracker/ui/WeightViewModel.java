package de.steffzilla.weighttracker.ui;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.util.Event;

public class WeightViewModel extends ViewModel {

    private final WeightRepository repository;
    private final Executor executor;

    private final MutableLiveData<Event<Integer>> userMessage = new MutableLiveData<>();

    public WeightViewModel(WeightRepository repository, Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public LiveData<List<WeightEntry>> getAllEntries() {
        return repository.getAllEntries();
    }

    /** Emits a string resource id to be shown to the user (e.g. via Snackbar). */
    public LiveData<Event<Integer>> getUserMessage() {
        return userMessage;
    }

    public void addEntry(LocalDate date, float weightKg) {
        if (date.isAfter(LocalDate.now())) {
            postMessage(R.string.error_date_future);
            return;
        }
        executor.execute(() -> {
            if (repository.existsForDate(date)) {
                postMessage(R.string.error_date_duplicate);
                return;
            }
            repository.insert(new WeightEntry(date, weightKg));
        });
    }

    public void updateEntry(WeightEntry entry) {
        executor.execute(() -> {
            if (repository.existsForDateExcluding(entry.getDate(), entry.getId())) {
                postMessage(R.string.error_date_duplicate);
                return;
            }
            repository.update(entry);
        });
    }

    public void deleteEntry(WeightEntry entry) {
        executor.execute(() -> repository.delete(entry));
    }

    private void postMessage(@StringRes int messageResId) {
        userMessage.postValue(new Event<>(messageResId));
    }
}