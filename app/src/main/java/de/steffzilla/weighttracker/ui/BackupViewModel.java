package de.steffzilla.weighttracker.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
 * Orchestrates CSV export/import off the UI thread. The Activity resolves the
 * user-picked Storage Access Framework document to a stream and hands it over; this
 * ViewModel takes ownership of the stream (closing it when done), runs the pure
 * {@link WeightCsvCodec}/{@link ImportPlanner} logic and the Room writes, and posts a
 * one-shot {@link BackupMessage} describing the outcome.
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

    /** Writes all entries (newest first) to the stream as CSV, then closes the stream. */
    public void export(OutputStream out) {
        executor.execute(() -> {
            try (OutputStream os = out) {
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

    /** Validates and (only if fully valid) imports CSV from the stream, then closes it. */
    public void importFrom(InputStream in) {
        executor.execute(() -> {
            try (InputStream is = in) {
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

    private void post(BackupMessage msg) {
        message.postValue(new Event<>(msg));
    }
}