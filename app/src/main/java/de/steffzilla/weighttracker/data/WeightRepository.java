package de.steffzilla.weighttracker.data;

import android.database.sqlite.SQLiteConstraintException;

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

    /**
     * Writes a validated set of imported entries in a single transaction.
     *
     * <p>Deliberately not constraint-safe like {@link #insert(WeightEntry)}: a manual add
     * that claims one of these dates after the import was planned makes the whole
     * transaction fail, which is the all-or-nothing behaviour the import promises. The
     * caller reports it as a failed import.
     */
    public void importEntries(List<WeightEntry> entries) {
        dao.insertAll(entries);
    }

    /**
     * Inserts a new entry.
     *
     * @return {@code false} if the write violated a constraint. The unique index on
     *         {@code date} is the only one an entry built by the app can hit, so this
     *         means the date is taken. Callers check {@link #existsForDate} first, but
     *         nothing holds the date between that check and this write — a CSV import
     *         runs on its own thread and can claim it in between, and rejecting the entry
     *         is what the user would have been told anyway.
     */
    public boolean insert(WeightEntry entry) {
        try {
            dao.insert(entry);
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    /**
     * Writes a changed entry.
     *
     * @return {@code false} if another entry took the new date in the meantime; see
     *         {@link #insert(WeightEntry)}.
     */
    public boolean update(WeightEntry entry) {
        try {
            dao.update(entry);
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        }
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