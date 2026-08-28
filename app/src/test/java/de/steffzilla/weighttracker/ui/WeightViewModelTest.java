package de.steffzilla.weighttracker.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;

@RunWith(MockitoJUnitRunner.class)
public class WeightViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    WeightRepository repository;

    /**
     * Deliberately years ahead of the wall clock, so "is this date in the future?" can
     * only be answered from the injected one — a real clock would reject every date here
     * as far in the future.
     */
    private static final LocalDate TODAY = LocalDate.of(2030, 6, 15);

    private WeightViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new WeightViewModel(repository, Runnable::run, () -> TODAY);
    }

    @Test
    public void addEntry_futureDate_postsErrorAndSkipsInsert() {
        viewModel.addEntry(TODAY.plusDays(1), 80.0f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).insert(any());
    }

    /**
     * The clock's own day is not "after today". Reading the wall clock instead would
     * reject this date as years away, which is what pins the injection.
     */
    @Test
    public void addEntry_theClocksOwnDay_isAccepted() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);

        viewModel.addEntry(TODAY, 80.0f);

        verify(repository).insert(any());
        assertNull(viewModel.getUserMessage().getValue());
    }

    /** A day before the clock's, still years ahead of the wall clock. */
    @Test
    public void addEntry_aDayBeforeTheClock_isAccepted() {
        LocalDate yesterday = TODAY.minusDays(1);
        when(repository.existsForDate(yesterday)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);

        viewModel.addEntry(yesterday, 80.0f);

        verify(repository).insert(any());
        assertNull(viewModel.getUserMessage().getValue());
    }

    @Test
    public void addEntry_duplicateDate_postsErrorAndSkipsInsert() {
        when(repository.existsForDate(TODAY)).thenReturn(true);
        viewModel.addEntry(TODAY, 80.0f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).insert(any());
    }

    @Test
    public void addEntry_validData_insertsEntry() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);
        viewModel.addEntry(TODAY, 84.5f);
        verify(repository).insert(argThat(e ->
                e.getDate().equals(TODAY) && e.getWeightKg() == 84.5f));
    }

    @Test
    public void addEntry_validData_noErrorMessage() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);
        viewModel.addEntry(TODAY, 84.5f);
        assertNull(viewModel.getUserMessage().getValue());
    }

    /**
     * The defensive path: the unique index rejected the row even though the check said the
     * date was free. All writes share one executor, so this is not reachable as things
     * stand — it is what keeps a second writer from ever becoming a crash.
     */
    @Test
    public void addEntry_dateTakenBetweenCheckAndInsert_postsError() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(false);
        viewModel.addEntry(TODAY, 84.5f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
    }

    @Test
    public void updateEntry_conflictingDate_postsErrorAndSkipsUpdate() {
        LocalDate date = TODAY.minusDays(1);
        WeightEntry entry = new WeightEntry(date, 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(date, 1L)).thenReturn(true);
        viewModel.updateEntry(entry);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).update(any());
    }

    @Test
    public void updateEntry_validData_updatesEntry() {
        LocalDate date = TODAY.minusDays(1);
        WeightEntry entry = new WeightEntry(date, 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(date, 1L)).thenReturn(false);
        when(repository.update(entry)).thenReturn(true);
        viewModel.updateEntry(entry);
        verify(repository).update(entry);
        assertNull(viewModel.getUserMessage().getValue());
    }

    @Test
    public void updateEntry_dateTakenBetweenCheckAndUpdate_postsError() {
        LocalDate date = TODAY.minusDays(1);
        WeightEntry entry = new WeightEntry(date, 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(date, 1L)).thenReturn(false);
        when(repository.update(entry)).thenReturn(false);
        viewModel.updateEntry(entry);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
    }

    // ---- "entry added", the signal the entry list navigates on ----

    @Test
    public void addEntry_storedEntry_announcesIt() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);

        viewModel.addEntry(TODAY, 84.5f);

        assertNotNull(viewModel.getEntryAdded().getValue().getContentIfNotConsumed());
    }

    @Test
    public void addEntry_futureDate_announcesNothing() {
        viewModel.addEntry(TODAY.plusDays(1), 84.5f);

        assertNull(viewModel.getEntryAdded().getValue());
    }

    @Test
    public void addEntry_duplicateDate_announcesNothing() {
        when(repository.existsForDate(TODAY)).thenReturn(true);

        viewModel.addEntry(TODAY, 84.5f);

        assertNull(viewModel.getEntryAdded().getValue());
    }

    /** A rejected write must not look like a stored entry either. */
    @Test
    public void addEntry_insertRejected_announcesNothing() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(false);

        viewModel.addEntry(TODAY, 84.5f);

        assertNull(viewModel.getEntryAdded().getValue());
    }

    /** Correcting an existing entry is not capturing a new measurement. */
    @Test
    public void updateEntry_announcesNothing() {
        WeightEntry entry = new WeightEntry(TODAY.minusDays(1), 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(entry.getDate(), 1L)).thenReturn(false);
        when(repository.update(entry)).thenReturn(true);

        viewModel.updateEntry(entry);

        assertNull(viewModel.getEntryAdded().getValue());
    }

    /** Consumed once, so returning from the trend screen must not send you back to it. */
    @Test
    public void entryAdded_isConsumedOnlyOnce() {
        when(repository.existsForDate(TODAY)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);
        viewModel.addEntry(TODAY, 84.5f);

        viewModel.getEntryAdded().getValue().getContentIfNotConsumed();

        assertNull(viewModel.getEntryAdded().getValue().getContentIfNotConsumed());
    }

    @Test
    public void deleteEntry_callsRepositoryDelete() {
        WeightEntry entry = new WeightEntry(TODAY.minusDays(1), 80.0f);
        viewModel.deleteEntry(entry);
        verify(repository).delete(entry);
    }
}