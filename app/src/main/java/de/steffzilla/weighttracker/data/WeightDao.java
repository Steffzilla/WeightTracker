package de.steffzilla.weighttracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WeightDao {

    @Insert
    long insert(WeightEntry entry);

    @Update
    void update(WeightEntry entry);

    @Delete
    void delete(WeightEntry entry);

    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    LiveData<List<WeightEntry>> getAllEntries();

    @Query("SELECT * FROM weight_entries WHERE id = :id")
    WeightEntry getById(long id);

    @Query("SELECT COUNT(*) FROM weight_entries WHERE date = :epochDay")
    int countByDate(long epochDay);

    @Query("SELECT COUNT(*) FROM weight_entries WHERE date = :epochDay AND id != :excludeId")
    int countByDateExcluding(long epochDay, long excludeId);
}