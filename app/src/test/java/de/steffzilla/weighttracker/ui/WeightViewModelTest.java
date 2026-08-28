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

    private WeightViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new WeightViewModel(repository, Runnable::run);
    }

    @Test
    public void addEntry_futureDate_postsErrorAndSkipsInsert() {
        viewModel.addEntry(LocalDate.now().plusDays(1), 80.0f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).insert(any());
    }

    @Test
    public void addEntry_duplicateDate_postsErrorAndSkipsInsert() {
        LocalDate today = LocalDate.now();
        when(repository.existsForDate(today)).thenReturn(true);
        viewModel.addEntry(today, 80.0f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).insert(any());
    }

    @Test
    public void addEntry_validData_insertsEntry() {
        LocalDate today = LocalDate.now();
        when(repository.existsForDate(today)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);
        viewModel.addEntry(today, 84.5f);
        verify(repository).insert(argThat(e ->
                e.getDate().equals(today) && e.getWeightKg() == 84.5f));
    }

    @Test
    public void addEntry_validData_noErrorMessage() {
        LocalDate today = LocalDate.now();
        when(repository.existsForDate(today)).thenReturn(false);
        when(repository.insert(any())).thenReturn(true);
        viewModel.addEntry(today, 84.5f);
        assertNull(viewModel.getUserMessage().getValue());
    }

    /**
     * The defensive path: the unique index rejected the row even though the check said the
     * date was free. All writes share one executor, so this is not reachable as things
     * stand — it is what keeps a second writer from ever becoming a crash.
     */
    @Test
    public void addEntry_dateTakenBetweenCheckAndInsert_postsError() {
        LocalDate today = LocalDate.now();
        when(repository.existsForDate(today)).thenReturn(false);
        when(repository.insert(any())).thenReturn(false);
        viewModel.addEntry(today, 84.5f);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
    }

    @Test
    public void updateEntry_conflictingDate_postsErrorAndSkipsUpdate() {
        LocalDate date = LocalDate.now().minusDays(1);
        WeightEntry entry = new WeightEntry(date, 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(date, 1L)).thenReturn(true);
        viewModel.updateEntry(entry);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
        verify(repository, never()).update(any());
    }

    @Test
    public void updateEntry_validData_updatesEntry() {
        LocalDate date = LocalDate.now().minusDays(1);
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
        LocalDate date = LocalDate.now().minusDays(1);
        WeightEntry entry = new WeightEntry(date, 80.0f);
        entry.setId(1L);
        when(repository.existsForDateExcluding(date, 1L)).thenReturn(false);
        when(repository.update(entry)).thenReturn(false);
        viewModel.updateEntry(entry);
        assertNotNull(viewModel.getUserMessage().getValue().getContentIfNotConsumed());
    }

    @Test
    public void deleteEntry_callsRepositoryDelete() {
        WeightEntry entry = new WeightEntry(LocalDate.now().minusDays(1), 80.0f);
        viewModel.deleteEntry(entry);
        verify(repository).delete(entry);
    }
}