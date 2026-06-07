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
        viewModel.addEntry(today, 84.5f);
        verify(repository).insert(argThat(e ->
                e.getDate().equals(today) && e.getWeightKg() == 84.5f));
    }

    @Test
    public void addEntry_validData_noErrorMessage() {
        LocalDate today = LocalDate.now();
        when(repository.existsForDate(today)).thenReturn(false);
        viewModel.addEntry(today, 84.5f);
        assertNull(viewModel.getUserMessage().getValue());
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
        viewModel.updateEntry(entry);
        verify(repository).update(entry);
    }

    @Test
    public void deleteEntry_callsRepositoryDelete() {
        WeightEntry entry = new WeightEntry(LocalDate.now().minusDays(1), 80.0f);
        viewModel.deleteEntry(entry);
        verify(repository).delete(entry);
    }
}