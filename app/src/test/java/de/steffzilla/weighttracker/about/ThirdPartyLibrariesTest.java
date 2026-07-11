package de.steffzilla.weighttracker.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guards the hand-maintained third-party license list: it must be non-empty, every entry
 * fully populated, every license within the project's allowed set, and no library listed
 * twice.
 */
public class ThirdPartyLibrariesTest {

    /** Licenses permitted by the project's third-party policy (see CLAUDE.md). */
    private static final Set<String> ALLOWED_LICENSES =
            Set.of("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "Public-Domain");

    @Test
    public void listIsNotEmpty() {
        assertFalse(ThirdPartyLibraries.all().isEmpty());
    }

    @Test
    public void everyEntryIsFullyPopulated() {
        for (ThirdPartyLibrary lib : ThirdPartyLibraries.all()) {
            assertFalse("name blank", lib.name().isBlank());
            assertFalse("version blank for " + lib.name(), lib.version().isBlank());
            assertFalse("license blank for " + lib.name(), lib.license().isBlank());
        }
    }

    @Test
    public void everyLicenseIsPolicyCompliant() {
        for (ThirdPartyLibrary lib : ThirdPartyLibraries.all()) {
            assertTrue("disallowed license '" + lib.license() + "' for " + lib.name(),
                    ALLOWED_LICENSES.contains(lib.license()));
        }
    }

    @Test
    public void libraryNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ThirdPartyLibrary lib : ThirdPartyLibraries.all()) {
            assertTrue("duplicate library " + lib.name(), seen.add(lib.name()));
        }
    }

    @Test
    public void listIsImmutable() {
        List<ThirdPartyLibrary> libs = ThirdPartyLibraries.all();
        assertThrows(UnsupportedOperationException.class,
                () -> libs.add(new ThirdPartyLibrary("x", "1", "MIT")));
    }
}
