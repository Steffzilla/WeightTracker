package de.steffzilla.weighttracker.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

@Entity(
        tableName = "weight_entries",
        indices = {@Index(value = {"date"}, unique = true)}
)
public class WeightEntry {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private LocalDate date;

    private float weightKg;

    public WeightEntry(@NonNull LocalDate date, float weightKg) {
        this.date = date;
        this.weightKg = weightKg;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public LocalDate getDate() { return date; }
    public void setDate(@NonNull LocalDate date) { this.date = date; }

    public float getWeightKg() { return weightKg; }
    public void setWeightKg(float weightKg) { this.weightKg = weightKg; }
}