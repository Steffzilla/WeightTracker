package de.steffzilla.weighttracker.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

/**
 * Covers what the repository adds on top of the DAO: turning the unique-date index into a
 * return value instead of an exception. Needs a real database, because
 * {@link android.database.sqlite.SQLiteConstraintException} is only ever raised by SQLite.
 */
@RunWith(AndroidJUnit4.class)
public class WeightRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);

    private AppDatabase db;
    private WeightRepository repository;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class).build();
        repository = new WeightRepository(db.weightDao());
    }

    @After
    public void teardown() {
        db.close();
    }

    @Test
    public void insert_freeDate_reportsSuccess() {
        assertTrue(repository.insert(new WeightEntry(DATE, 84.5f)));
    }

    @Test
    public void insert_takenDate_reportsFailureWithoutThrowing() {
        repository.insert(new WeightEntry(DATE, 84.5f));

        assertFalse(repository.insert(new WeightEntry(DATE, 85.0f)));
    }

    @Test
    public void insert_takenDate_leavesTheExistingEntryUntouched() {
        repository.insert(new WeightEntry(DATE, 84.5f));

        repository.insert(new WeightEntry(DATE, 85.0f));

        var all = repository.getAllEntriesSnapshot();
        assertEquals(1, all.size());
        assertEquals(84.5f, all.get(0).getWeightKg(), 0.01f);
    }

    @Test
    public void update_ontoTakenDate_reportsFailureWithoutThrowing() {
        repository.insert(new WeightEntry(DATE, 84.5f));
        repository.insert(new WeightEntry(DATE.plusDays(1), 85.0f));
        WeightEntry moved = repository.getAllEntriesSnapshot().get(0); // the later date
        moved.setDate(DATE);

        assertFalse(repository.update(moved));
    }

    @Test
    public void update_ontoFreeDate_reportsSuccess() {
        repository.insert(new WeightEntry(DATE, 84.5f));
        WeightEntry entry = repository.getAllEntriesSnapshot().get(0);
        entry.setWeightKg(83.0f);

        assertTrue(repository.update(entry));
    }
}
