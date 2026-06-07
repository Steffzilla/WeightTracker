package de.steffzilla.weighttracker.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

@RunWith(AndroidJUnit4.class)
public class WeightDaoTest {

    private AppDatabase db;
    private WeightDao dao;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class).build();
        dao = db.weightDao();
    }

    @After
    public void teardown() {
        db.close();
    }

    @Test
    public void insert_thenCountByDate_returnsOne() {
        dao.insert(new WeightEntry(LocalDate.of(2026, 6, 1), 84.5f));
        assertEquals(1, dao.countByDate(LocalDate.of(2026, 6, 1).toEpochDay()));
    }

    @Test
    public void countByDate_missingDate_returnsZero() {
        assertEquals(0, dao.countByDate(LocalDate.of(2020, 1, 1).toEpochDay()));
    }

    @Test
    public void insert_thenGetById_returnsCorrectEntry() {
        WeightEntry entry = new WeightEntry(LocalDate.of(2026, 6, 2), 83.2f);
        long id = dao.insert(entry);
        WeightEntry loaded = dao.getById(id);
        assertNotNull(loaded);
        assertEquals(83.2f, loaded.getWeightKg(), 0.01f);
        assertEquals(LocalDate.of(2026, 6, 2), loaded.getDate());
    }

    @Test
    public void update_changesWeightKg() {
        WeightEntry entry = new WeightEntry(LocalDate.of(2026, 6, 3), 85.0f);
        long id = dao.insert(entry);
        entry.setId(id);
        entry.setWeightKg(84.0f);
        dao.update(entry);
        WeightEntry updated = dao.getById(id);
        assertEquals(84.0f, updated.getWeightKg(), 0.01f);
    }

    @Test
    public void delete_removesEntry() {
        WeightEntry entry = new WeightEntry(LocalDate.of(2026, 6, 4), 83.0f);
        long id = dao.insert(entry);
        entry.setId(id);
        dao.delete(entry);
        assertEquals(0, dao.countByDate(LocalDate.of(2026, 6, 4).toEpochDay()));
    }

    @Test
    public void uniqueDateConstraint_preventsSecondInsert() {
        LocalDate date = LocalDate.of(2026, 6, 5);
        dao.insert(new WeightEntry(date, 84.0f));
        try {
            dao.insert(new WeightEntry(date, 85.0f));
            fail("SQLiteConstraintException expected for duplicate date");
        } catch (Exception expected) {
            // unique constraint correctly enforced
        }
        assertEquals(1, dao.countByDate(date.toEpochDay()));
    }
}