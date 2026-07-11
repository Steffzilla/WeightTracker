package de.steffzilla.weighttracker.about;

/**
 * Immutable description of one bundled third-party library, shown on the About screen.
 *
 * <p>The list is hand-maintained (see {@link ThirdPartyLibraries}) rather than generated
 * by Google's oss-licenses Gradle plugin, which would pull in Play Services — a
 * dependency the project deliberately avoids.
 *
 * @param name    human-readable library name
 * @param version bundled version, kept in sync with {@code gradle/libs.versions.toml}
 * @param license SPDX-style license identifier (e.g. {@code "Apache-2.0"})
 */
public record ThirdPartyLibrary(String name, String version, String license) {
}
