package de.steffzilla.weighttracker.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.backup.ImportPlan;
import de.steffzilla.weighttracker.backup.ImportPlanner;
import de.steffzilla.weighttracker.backup.ImportResult;
import de.steffzilla.weighttracker.backup.ParsedEntry;
import de.steffzilla.weighttracker.backup.WeightCsvCodec;
import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.data.WeightRepository;
import de.steffzilla.weighttracker.util.Event;

/**
 * Orchestrates CSV export/import off the UI thread. The Activity hands over a
 * {@link Callable} that opens the user-picked Storage Access Framework document; opening
 * it can block for seconds on a cloud provider, so it happens on the executor like
 * everything else here. This ViewModel takes ownership of the stream (closing it when
 * done), runs the pure {@link WeightCsvCodec}/{@link ImportPlanner} logic and the Room
 * writes, and posts a one-shot {@link BackupMessage} describing the outcome.
 *
 * <p>Import is all-or-nothing: if any line is malformed, or any date already exists with
 * a different weight, nothing is written. Rows whose date and weight already match an
 * existing entry are silently skipped as no-ops.
 */
public class BackupViewModel extends ViewModel {

    private final WeightRepository repository;
    private final Executor executor;
    private final WeightCsvCodec codec = new WeightCsvCodec();

    private final MutableLiveData<Event<BackupMessage>> message = new MutableLiveData<>();

    public BackupViewModel(WeightRepository repository, Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    /** Emits a one-shot message describing the result of an export or import. */
    public LiveData<Event<BackupMessage>> getMessage() {
        return message;
    }

    /** Writes all entries (newest first) as CSV to the opened document, then closes it. */
    public void export(Callable<OutputStream> opener) {
        executor.execute(() -> {
            OutputStream opened = open(opener);
            if (opened == null) {
                return;
            }
            try (OutputStream os = opened) {
                List<WeightEntry> all = repository.getAllEntriesSnapshot();
                if (all.isEmpty()) {
                    post(BackupMessage.plain(R.string.backup_export_empty));
                    return;
                }
                os.write(codec.encode(all).getBytes(StandardCharsets.UTF_8));
                os.flush();
                post(BackupMessage.quantity(R.plurals.backup_export_success, all.size(), all.size()));
            } catch (Exception e) {
                post(BackupMessage.plain(R.string.backup_export_failed));
            }
        });
    }

    /** Validates and (only if fully valid) imports CSV from the opened document, then closes it. */
    public void importFrom(Callable<InputStream> opener) {
        executor.execute(() -> {
            InputStream opened = open(opener);
            if (opened == null) {
                return;
            }
            try (InputStream is = opened) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                ImportResult parsed = codec.decode(content);
                if (parsed.hasErrors()) {
                    post(BackupMessage.quantity(R.plurals.backup_import_format_error,
                            parsed.errorLines().size(), parsed.errorLines().size()));
                    return;
                }
                if (parsed.entries().isEmpty()) {
                    post(BackupMessage.plain(R.string.backup_import_empty));
                    return;
                }

                List<WeightEntry> existing = repository.getAllEntriesSnapshot();
                ImportPlan plan = ImportPlanner.plan(existing, parsed.entries());
                if (plan.hasCollisions()) {
                    post(BackupMessage.quantity(R.plurals.backup_import_collision,
                            plan.collisions().size(), plan.collisions().size()));
                    return;
                }
                if (plan.toInsert().isEmpty()) {
                    post(BackupMessage.quantity(R.plurals.backup_import_nothing_new,
                            plan.identicalSkipped(), plan.identicalSkipped()));
                    return;
                }

                List<WeightEntry> toInsert = new ArrayList<>();
                for (ParsedEntry entry : plan.toInsert()) {
                    toInsert.add(new WeightEntry(entry.date(), entry.weightKg()));
                }
                repository.importEntries(toInsert);
                post(BackupMessage.quantity(R.plurals.backup_import_success,
                        toInsert.size(), toInsert.size(), plan.identicalSkipped()));
            } catch (Exception e) {
                post(BackupMessage.plain(R.string.backup_import_failed));
            }
        });
    }

    /**
     * Opens the picked document on the calling (background) thread, or reports that it
     * could not be opened and returns {@code null}. A provider that throws and one that
     * hands back no stream are the same failure to the user, and a different one from a
     * malformed file or a failed write.
     */
    private <T extends Closeable> T open(Callable<T> opener) {
        try {
            T stream = opener.call();
            if (stream != null) {
                return stream;
            }
        } catch (Exception e) {
            // fall through to the same message as a null stream
        }
        post(BackupMessage.plain(R.string.backup_io_error));
        return null;
    }

    private void post(BackupMessage msg) {
        message.postValue(new Event<>(msg));
    }
}