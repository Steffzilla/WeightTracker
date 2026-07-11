package de.steffzilla.weighttracker.about;

import java.util.List;

/**
 * Hand-maintained catalogue of the third-party libraries shipped in the app, used to
 * render the license section of the About screen.
 *
 * <p>Only libraries whose code is actually bundled into the released APK are listed here
 * (the {@code implementation} dependencies). Test-only dependencies such as JUnit,
 * Mockito and Espresso are not distributed and are therefore intentionally omitted.
 *
 * <p>Versions mirror {@code gradle/libs.versions.toml}; update this list whenever a
 * runtime dependency is added, removed, or version-bumped.
 */
public final class ThirdPartyLibraries {

    private static final List<ThirdPartyLibrary> LIBRARIES = List.of(
            new ThirdPartyLibrary("AndroidX AppCompat", "1.6.1", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX Activity", "1.8.0", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX ConstraintLayout", "2.1.4", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX RecyclerView", "1.3.2", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX Room", "2.6.1", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX Lifecycle", "2.8.7", "Apache-2.0"),
            new ThirdPartyLibrary("AndroidX Preference", "1.2.1", "Apache-2.0"),
            new ThirdPartyLibrary("Material Components for Android", "1.10.0", "Apache-2.0"));

    private ThirdPartyLibraries() {
    }

    /** @return the bundled libraries, in display order; never empty, never modifiable. */
    public static List<ThirdPartyLibrary> all() {
        return LIBRARIES;
    }
}
