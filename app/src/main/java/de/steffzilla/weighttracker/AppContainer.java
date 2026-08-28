package de.steffzilla.weighttracker;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import de.steffzilla.weighttracker.data.AppDatabase;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.stats.WeightStatisticsCalculator;

/**
 * The app's composition root: the one place that decides what the ViewModels are built
 * from. Held by {@link WeightTrackerApplication}, so it lives as long as the process.
 *
 * <p>That lifetime is what makes the single background executor safe to never shut down,
 * and sharing it across every screen means writes from the entry list and from a CSV
 * import cannot interleave — they queue behind one another on the same thread.
 *
 * <p>Accepted cost of that single thread: the backup screen also opens the picked
 * document on it, which a cloud DocumentsProvider can stall for seconds, and a new entry
 * would then wait. The two are mutually exclusive in practice — while the file picker is
 * open the user is not entering a weight — so one thread is worth more than the second
 * executor it would take to separate file I/O from database work.
 */
public class AppContainer {

    private final WeightRepository repository;
    private final Executor databaseExecutor;
    private final WeightStatisticsCalculator calculator;
    private final Supplier<LocalDate> today;

    public AppContainer(Context context) {
        this(new WeightRepository(AppDatabase.getInstance(context).weightDao()),
                Executors.newSingleThreadExecutor(),
                new WeightStatisticsCalculator(),
                LocalDate::now);
    }

    /** Lets a unit test assemble a container without an Android {@link Context}. */
    @VisibleForTesting
    public AppContainer(WeightRepository repository,
                        Executor databaseExecutor,
                        WeightStatisticsCalculator calculator,
                        Supplier<LocalDate> today) {
        this.repository = repository;
        this.databaseExecutor = databaseExecutor;
        this.calculator = calculator;
        this.today = today;
    }

    public WeightRepository repository() {
        return repository;
    }

    public Executor databaseExecutor() {
        return databaseExecutor;
    }

    public WeightStatisticsCalculator calculator() {
        return calculator;
    }

    public Supplier<LocalDate> today() {
        return today;
    }
}
