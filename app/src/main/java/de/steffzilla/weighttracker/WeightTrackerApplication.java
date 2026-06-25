package de.steffzilla.weighttracker;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import de.steffzilla.weighttracker.settings.ThemeMode;

/**
 * Applies the persisted theme preference before any activity is created, so the chosen
 * light/dark mode is in effect from the first frame. The thin framework mapping lives
 * here; the value-to-enum resolution it relies on is unit-tested in {@link ThemeMode}.
 */
public class WeightTrackerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String value = prefs.getString(ThemeMode.PREF_KEY, ThemeMode.SYSTEM.value());
        AppCompatDelegate.setDefaultNightMode(nightModeFor(ThemeMode.fromValue(value)));
    }

    /** Maps a {@link ThemeMode} to the corresponding {@link AppCompatDelegate} constant. */
    public static int nightModeFor(ThemeMode mode) {
        return switch (mode) {
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
    }
}