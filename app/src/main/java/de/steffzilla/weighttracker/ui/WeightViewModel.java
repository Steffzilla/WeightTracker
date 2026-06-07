package de.steffzilla.weighttracker.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.util.Event;

public class WeightViewModel extends ViewModel {

    private final WeightRepository repository;
    private final Executor executor;

    private final MutableLiveData<Event<String>> userMessage = new MutableLiveData<>();

    public WeightViewModel(WeightRepository repository, Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public LiveData<List<WeightEntry>> getAllEntries() {
        return repository.getAllEntries();
    }

    public LiveData<Event<String>> getUserMessage() {
        return userMessage;
    }

    public void addEntry(LocalDate date, float weightKg) {
        if (date.isAfter(LocalDate.now())) {
            userMessage.setValue(new Event<>("Datum darf nicht in der Zukunft liegen."));
            return;
        }
        executor.execute(() -> {
            if (repository.existsForDate(date)) {
                userMessage.postValue(new Event<>("Für dieses Datum existiert bereits ein Eintrag."));
                return;
            }
            repository.insert(new WeightEntry(date, weightKg));
        });
    }

    public void updateEntry(WeightEntry entry) {
        executor.execute(() -> {
            if (repository.existsForDateExcluding(entry.getDate(), entry.getId())) {
                userMessage.postValue(new Event<>("Für dieses Datum existiert bereits ein Eintrag."));
                return;
            }
            repository.update(entry);
        });
    }

    public void deleteEntry(WeightEntry entry) {
        executor.execute(() -> repository.delete(entry));
    }
}