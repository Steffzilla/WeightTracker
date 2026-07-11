package de.steffzilla.weighttracker.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.about.ThirdPartyLibraries;
import de.steffzilla.weighttracker.about.ThirdPartyLibrary;
import de.steffzilla.weighttracker.databinding.ActivityAboutBinding;
import de.steffzilla.weighttracker.databinding.ItemAboutLicenseBinding;

/**
 * Static "Über" screen: app name, version, copyright, a health disclaimer, and the
 * hand-maintained list of bundled third-party libraries and their licenses. The license
 * rows are built from the framework-free {@link ThirdPartyLibraries} catalogue so the
 * data stays unit-testable; this Activity only renders it and reads the version name
 * from the {@link PackageManager}.
 */
public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        applyWindowInsets();
        binding.textVersion.setText(getString(R.string.about_version, resolveVersionName()));
        renderLicenses();
    }

    private String resolveVersionName() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return name != null ? name : getString(R.string.about_version_unknown);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.about_version_unknown);
        }
    }

    private void renderLicenses() {
        LayoutInflater inflater = getLayoutInflater();
        for (ThirdPartyLibrary lib : ThirdPartyLibraries.all()) {
            ItemAboutLicenseBinding row =
                    ItemAboutLicenseBinding.inflate(inflater, binding.licenseList, false);
            row.textLibraryName.setText(
                    getString(R.string.about_license_name, lib.name(), lib.version()));
            row.textLibraryLicense.setText(lib.license());
            binding.licenseList.addView(row.getRoot());
        }
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
