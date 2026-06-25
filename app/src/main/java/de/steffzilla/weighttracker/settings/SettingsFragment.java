package de.steffzilla.weighttracker.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.WeightTrackerApplication;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference themePreference = findPreference(ThemeMode.PREF_KEY);
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeMode mode = ThemeMode.fromValue((String) newValue);
                AppCompatDelegate.setDefaultNightMode(WeightTrackerApplication.nightModeFor(mode));
                return true;
            });
        }
    }
}