package de.steffzilla.weighttracker.data;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.util.List;

public class WeightRepository {

    private final WeightDao dao;

    public WeightRepository(WeightDao dao) {
        this.dao = dao;
    }

    public LiveData<List<WeightEntry>> getAllEntries() {
        return dao.getAllEntries();
    }

    /** Synchronous snapshot (newest first) for backup export/import; call off the UI thread. */
    public List<WeightEntry> getAllEntriesSnapshot() {
        return dao.getAllEntriesSnapshot();
    }

    /** Writes a validated set of imported entries in a single transaction. */
    public void importEntries(List<WeightEntry> entries) {
        dao.insertAll(entries);
    }

    public long insert(WeightEntry entry) {
        return dao.insert(entry);
    }

    public void update(WeightEntry entry) {
        dao.update(entry);
    }

    public void delete(WeightEntry entry) {
        dao.delete(entry);
    }

    public boolean existsForDate(LocalDate date) {
        return dao.countByDate(date.toEpochDay()) > 0;
    }

    public boolean existsForDateExcluding(LocalDate date, long excludeId) {
        return dao.countByDateExcluding(date.toEpochDay(), excludeId) > 0;
    }
}