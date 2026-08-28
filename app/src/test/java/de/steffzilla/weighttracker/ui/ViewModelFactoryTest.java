package de.steffzilla.weighttracker.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import de.steffzilla.weighttracker.AppContainer;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.stats.WeightStatisticsCalculator;

/**
 * The factory is the app's one wiring point, so what it must get right is dispatch:
 * every screen's ViewModel, and a clear failure for anything else.
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewModelFactoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    WeightRepository repository;

    private ViewModelFactory factory;

    @Before
    public void setup() {
        var container = new AppContainer(repository, Runnable::run,
                new WeightStatisticsCalculator(), () -> LocalDate.of(2026, 6, 15));
        factory = new ViewModelFactory(container);
    }

    @Test
    public void buildsTheEntryListViewModel() {
        assertNotNull(factory.create(WeightViewModel.class));
    }

    @Test
    public void buildsTheStatisticsViewModel() {
        // This ViewModel observes the entry list from its constructor.
        when(repository.getAllEntries()).thenReturn(new MutableLiveData<>());

        assertNotNull(factory.create(StatisticsViewModel.class));
    }

    @Test
    public void buildsTheBackupViewModel() {
        assertNotNull(factory.create(BackupViewModel.class));
    }

    /** A ViewModel nobody wired up must say so, not come back as the wrong type. */
    @Test
    public void unknownViewModel_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(Unwired.class));
    }

    /**
     * The base type must be rejected too — matching on assignability rather than identity
     * would answer a request for {@code ViewModel.class} with whichever branch comes first.
     */
    @Test
    public void theBaseViewModelType_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(ViewModel.class));
    }

    static class Unwired extends ViewModel {
    }
}
