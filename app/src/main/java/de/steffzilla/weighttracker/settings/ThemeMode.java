package de.steffzilla.weighttracker.settings;

/**
 * The user-selectable theme options, decoupled from the Android framework so the
 * value-to-mode mapping stays unit-testable. The persisted preference value is the
 * {@link #value()} string; {@link #fromValue(String)} resolves it back to an enum and
 * falls back to {@link #SYSTEM} for unknown or missing values.
 */
public enum ThemeMode {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    /** SharedPreferences key shared by {@code preferences.xml} and the app startup glue. */
    public static final String PREF_KEY = "pref_theme";

    private final String value;

    ThemeMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ThemeMode fromValue(String value) {
        for (ThemeMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return SYSTEM;
    }
}