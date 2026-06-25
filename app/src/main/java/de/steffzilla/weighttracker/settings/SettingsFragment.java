package de.steffzilla.weighttracker.settings;

import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.WeightTrackerApplication;
import de.steffzilla.weighttracker.stats.WeightBounds;

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

        configureDecimalInput(findPreference(WeightBounds.PREF_KEY_LOWER));
        configureDecimalInput(findPreference(WeightBounds.PREF_KEY_UPPER));
    }

    /** Constrains a weight-bound field to a decimal keyboard. */
    private static void configureDecimalInput(@Nullable EditTextPreference preference) {
        if (preference != null) {
            preference.setOnBindEditTextListener(editText -> editText.setInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
        }
    }
}