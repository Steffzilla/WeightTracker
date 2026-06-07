# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```powershell
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Run unit tests (JVM, no device needed)
./gradlew test

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "de.steffzilla.weighttracker.ExampleUnitTest"
```

## Project Overview

Android weight-tracking app for logging daily body weight. Package: `de.steffzilla.weighttracker`. Single-module (`:app`), Java source.

**Target:** Pixel 8 Pro and other modern devices only — `minSdk 35` (Android 15), `targetSdk / compileSdk 36`. No backward compatibility concerns; use modern Android APIs freely.

**Toolchain:**
- AGP 9.2.1, Gradle 9.4.1, Java 17 source/target compatibility, daemon JVM 21
- Dependency versions managed via `gradle/libs.versions.toml` (version catalog)
- Configuration cache enabled (`gradle.properties`)

**Current state:** Greenfield project — only a single `MainActivity` with a placeholder UI. No database, navigation, or architectural pattern is implemented yet.

## Architecture & Testability

Follow the official Android Architecture Guide (MVVM / Production-ready architecture). Code testability is a primary requirement. Although this is a greenfield project, do not write monolithic code.

- **Separation of Concerns**: Strictly separate UI (Activities/Fragments), Presentation Logic (ViewModels), and Data (Repositories/Room DAOs).
- **Dependency Injection (DI)**: Do not hardcode dependencies. Design all classes (ViewModels, Repositories) using **Constructor Injection** so they can be easily mocked in Unit Tests.
- **Thread Management**: UI thread must remain unblocked. Use standard Java concurrency tools or explicit executors for background tasks (e.g., Room database operations).
- **State Management**: ViewModels must expose UI state using observable patterns (like `LiveData` or Java-compatible observable fields) to decouple the UI from business logic.

Key paths:
- `app/src/main/java/de/steffzilla/weighttracker/` — all source code
- `app/src/main/res/` — layouts, themes, XML configs
- `app/src/main/keepRules/rules.keep` — ProGuard/R8 keep rules (add entries here, not inline)

## Testing Strategy

Every new feature or bugfix must be accompanied by corresponding automated tests. Do not consider a task complete without tests.

- **Unit Tests (`app/src/test/`)**: Write Unit Tests for all pure Java logic, ViewModels, and Repositories. Use **Mockito** for mocking injected dependencies.
- **Instrumented Tests (`app/src/androidTest/`)**: Use Espresso/ActivityScenario only for critical UI flows or Room DAO integration tests that require an Android environment. Prefer Unit Tests wherever possible for speed.
- **Test-Driven Mindset**: When implementing a new business rule, ask to write the Unit Test first or simultaneously with the implementation.

## Java & Android Style Conventions

- **Modern Java**: Leverage Java 17 features where appropriate (e.g., `switch` expressions, `Records` for immutable data transfer objects, `var` for local variables if it improves readability).
- **View Binding**: Do not use `findViewById()`. Use **Android View Binding** for XML layouts. Ensure it is enabled in `build.gradle` and properly cleaned up in lifecycles.
- **Material 3**: Use semantic Material 3 color roles (e.g., `?attr/colorOnSurface`) in XML layouts instead of hardcoded hex colors to support Day/Night themes natively.
- **Resources**: Never hardcode strings, dimensions, or colors in Java or XML. Use `strings.xml`, `dimens.xml`, etc.

## Accessibility

The app must be usable with Android's accessibility services (TalkBack, font scaling, display size). Apply these rules consistently:

- **Content descriptions**: Every non-decorative `ImageView`, `ImageButton`, and icon-only button needs `android:contentDescription`. Purely decorative images get `android:importantForAccessibility="no"`.
- **Touch targets**: Interactive elements must be at least 48×48dp.
- **Semantics**: Use semantic widgets (`Button`, `CheckBox`, etc.) rather than click listeners on generic `View`s. Set `android:hint` on all input fields.
- **Color contrast**: Never rely on color alone to convey meaning. Material 3 color roles satisfy contrast requirements by default — do not override them with low-contrast custom values.
- **Font scaling**: Use `sp` for all text sizes so the system font scale is respected. Layouts must not clip or overlap text when the system font size is set to largest.
- **Focus order**: Verify logical focus traversal order in complex layouts. Use `android:nextFocusDown` / `android:nextFocusRight` only when the default order is wrong.

## Key Dependencies

| Library | Purpose |
|---|---|
| `androidx.appcompat` | AppCompat support |
| `androidx.activity:activity` | Activity lifecycle helpers |
| `androidx.constraintlayout` | UI layouts |
| `com.google.android.material` (Material 3) | UI components and theming |
| `junit:junit` + `androidx.test.espresso` | Unit and instrumented testing |
| `org.mockito:mockito-core` (MIT) | Mocking in unit tests |

Add new dependencies to `gradle/libs.versions.toml` first, then reference them via `libs.*` aliases in `app/build.gradle`.

## Third-party Library Policy

Before adding any new dependency, verify all three criteria:

1. **License**: Public Domain, Apache 2.0, MIT, BSD, or similar only. Copyleft licenses (GPL, LGPL, AGPL) are not acceptable.
2. **Maintenance**: The library must be actively maintained — recent releases, responsive issue tracker, multiple contributors. Abandoned or single-maintainer projects require explicit approval.
3. **Adoption**: Prefer libraries with broad industry adoption (e.g., AndroidX, Google, Square, JetBrains). Niche or experimental libraries require explicit approval.

If a task cannot be reasonably implemented without a library that violates one or more of these criteria, stop and present the user with the specific conflict and the available options before proceeding.

## Theme

Base theme is `Theme.Material3.DayNight.NoActionBar`. Light and night variants are in `res/values/themes.xml` and `res/values-night/themes.xml`. EdgeToEdge display is enabled in `MainActivity` — window insets must be applied manually for content that sits under system bars.

## Change Discipline

Each change must serve exactly one purpose. Do not mix unrelated modifications into the same edit or commit:

- **No collateral whitespace changes**: do not add, remove, or reformat whitespace (blank lines, indentation, trailing spaces, line endings) unless whitespace is the explicit goal.
- **No collateral comment changes**: do not remove, add, or reword comments — including commented-out code — unless that is the task.
- **No collateral style changes**: do not inline local variables, rename identifiers, reorder imports, or change method-call style (e.g. `Integer.valueOf` vs `Integer.parseInt`) as a side effect of another change.
- **Prefer Edit over Write**: use targeted edits instead of full-file rewrites. If a full rewrite is unavoidable, diff the result against the original and revert every line not covered by the task.

## Instructions
If anything in my instructions is unclear, ask instead of guessing.

## Maintaining this file
If you notice project-specific conventions, recurring patterns, or important constraints, suggest an update to this file proactively — without waiting to be asked.