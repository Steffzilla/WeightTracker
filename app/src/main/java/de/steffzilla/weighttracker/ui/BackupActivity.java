package de.steffzilla.weighttracker.ui;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.databinding.ActivityBackupBinding;

/**
 * Lets the user export all weight entries to a CSV file and import them back via the
 * Storage Access Framework (system file picker, including cloud providers). All data
 * work runs in {@link BackupViewModel}; this Activity only resolves the picked document
 * to a stream, wires the SAF launchers to the buttons, and shows the outcome as a
 * Snackbar.
 */
public class BackupActivity extends AppCompatActivity {

    private static final String EXPORT_MIME = "text/csv";
    private static final String[] IMPORT_MIMES = {
            "text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel"
    };

    private ActivityBackupBinding binding;
    private BackupViewModel viewModel;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(EXPORT_MIME),
                    this::onExportLocationChosen);

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onImportFileChosen);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        applyWindowInsets();
        setupViewModel();

        binding.buttonExport.setOnClickListener(v -> exportLauncher.launch(defaultExportName()));
        binding.buttonImport.setOnClickListener(v -> importLauncher.launch(IMPORT_MIMES));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new BackupViewModelFactory(this))
                .get(BackupViewModel.class);
        viewModel.getMessage().observe(this, event -> {
            BackupMessage msg = event.getContentIfNotConsumed();
            if (msg != null) {
                String text = msg.isQuantity()
                        ? getResources().getQuantityString(msg.resId(), msg.quantity(), msg.args())
                        : getString(msg.resId(), msg.args());
                Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void onExportLocationChosen(Uri uri) {
        if (uri == null) return; // user cancelled the picker
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                showIoError();
                return;
            }
            viewModel.export(os);
        } catch (Exception e) {
            showIoError();
        }
    }

    private void onImportFileChosen(Uri uri) {
        if (uri == null) return; // user cancelled the picker
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                showIoError();
                return;
            }
            viewModel.importFrom(is);
        } catch (Exception e) {
            showIoError();
        }
    }

    private void showIoError() {
        Snackbar.make(binding.getRoot(), R.string.backup_io_error, Snackbar.LENGTH_LONG).show();
    }

    private String defaultExportName() {
        return getString(R.string.backup_export_filename, LocalDate.now().toString());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentContainer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}