package de.steffzilla.weighttracker.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThemeModeTest {

    @Test
    public void fromValue_resolvesKnownValues() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("system"));
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromValue("light"));
        assertEquals(ThemeMode.DARK, ThemeMode.fromValue("dark"));
    }

    @Test
    public void fromValue_roundTripsEveryEnumValue() {
        for (ThemeMode mode : ThemeMode.values()) {
            assertEquals(mode, ThemeMode.fromValue(mode.value()));
        }
    }

    @Test
    public void fromValue_fallsBackToSystemForUnknown() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("midnight"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(""));
    }

    @Test
    public void fromValue_fallsBackToSystemForNull() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(null));
    }
}