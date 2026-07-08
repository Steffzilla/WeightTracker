package de.steffzilla.weighttracker.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;

@RunWith(MockitoJUnitRunner.class)
public class BackupViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    WeightRepository repository;

    @Captor
    ArgumentCaptor<List<WeightEntry>> entriesCaptor;

    private BackupViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new BackupViewModel(repository, Runnable::run);
    }

    private BackupMessage lastMessage() {
        return viewModel.getMessage().getValue().peekContent();
    }

    private ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ---- export ----

    @Test
    public void export_emptyDatabase_reportsEmptyAndWritesNothing() {
        when(repository.getAllEntriesSnapshot()).thenReturn(Collections.emptyList());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        viewModel.export(out);

        assertEquals(R.string.backup_export_empty, lastMessage().resId());
        assertEquals(0, out.size());
    }

    @Test
    public void export_writesCsvAndReportsCount() {
        when(repository.getAllEntriesSnapshot()).thenReturn(List.of(
                new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f),
                new WeightEntry(LocalDate.of(2026, 1, 2), 79.5f)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        viewModel.export(out);

        String written = out.toString(StandardCharsets.UTF_8);
        assertTrue(written.startsWith("date,weight_kg"));
        assertTrue(written.contains("2026-01-02,79.5"));
        assertEquals(R.plurals.backup_export_success, lastMessage().resId());
        assertEquals(2, lastMessage().quantity());
    }

    // ---- import ----

    @Test
    public void import_malformedFile_reportsFormatErrorAndDoesNotWrite() {
        viewModel.importFrom(csv("date,weight_kg\nnonsense\n"));

        assertEquals(R.plurals.backup_import_format_error, lastMessage().resId());
        verify(repository, never()).importEntries(anyList());
    }

    @Test
    public void import_collision_reportsCollisionAndDoesNotWrite() {
        when(repository.getAllEntriesSnapshot())
                .thenReturn(List.of(new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f)));

        viewModel.importFrom(csv("2026-01-01,81.0\n"));

        assertEquals(R.plurals.backup_import_collision, lastMessage().resId());
        verify(repository, never()).importEntries(anyList());
    }

    @Test
    public void import_allIdentical_reportsNothingNewAndDoesNotWrite() {
        when(repository.getAllEntriesSnapshot())
                .thenReturn(List.of(new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f)));

        viewModel.importFrom(csv("2026-01-01,80.0\n"));

        assertEquals(R.plurals.backup_import_nothing_new, lastMessage().resId());
        verify(repository, never()).importEntries(anyList());
    }

    @Test
    public void import_newEntries_writesThemAndReportsSuccess() {
        when(repository.getAllEntriesSnapshot())
                .thenReturn(List.of(new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f)));

        viewModel.importFrom(csv("2026-01-02,79.0\n2026-01-03,78.5\n"));

        verify(repository).importEntries(entriesCaptor.capture());
        assertEquals(2, entriesCaptor.getValue().size());
        assertEquals(R.plurals.backup_import_success, lastMessage().resId());
        assertEquals(2, lastMessage().quantity());
    }

    @Test
    public void import_emptyFile_reportsEmptyAndDoesNotWrite() {
        viewModel.importFrom(csv("date,weight_kg\n"));

        assertEquals(R.string.backup_import_empty, lastMessage().resId());
        verify(repository, never()).importEntries(any());
    }
}