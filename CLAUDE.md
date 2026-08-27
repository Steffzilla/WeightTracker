# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```powershell
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single test class (the umbrella task `test` does not accept --tests)
./gradlew :app:testDebugUnitTest --tests "de.steffzilla.weighttracker.stats.WeightStatisticsCalculatorTest"

# Run instrumented tests (requires connected device or emulator). adb/emulator are NOT
# on PATH — they live under ~/AppData/Local/Android/Sdk/{platform-tools,emulator}.
./gradlew connectedAndroidTest

# Run Android Lint — before every commit (see "Testing Strategy").
# The reasoning behind the settings is commented on the lint block in app/build.gradle.
./gradlew lintDebug
```

## Project Overview

Android weight-tracking app for logging daily body weight. Package: `de.steffzilla.weighttracker`. Single-module (`:app`), Java source.

**Target:** Pixel 8 Pro and other modern devices only — `minSdk 35` (Android 15). No backward compatibility concerns; use modern Android APIs freely.

**Toolchain:**
- Gradle build via AGP 9 (exact versions: `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`). **Three Java versions, all pinned to 21 and to be kept in step — do not conflate them:** the Gradle daemon JVM (`gradle/gradle-daemon-jvm.properties`), the toolchain that compiles and runs the unit tests (`app/build.gradle`), and the language level (`compileOptions`). 21 is the ceiling: the Kotlin compiler bundled with AGP 9 caps `jvmTarget` at 24, so Java 25 is rejected.
- Dependency versions managed via `gradle/libs.versions.toml` (version catalog)
- Configuration cache enabled (`gradle.properties`)

**App shape:** `MainActivity` shows the entry list (RecyclerView + FAB) with an Add/Edit bottom sheet and delete confirmation; `StatisticsActivity` shows the weight trend chart; `SettingsActivity` (theme, chart target band), `BackupActivity` (CSV export/import via SAF) and `AboutActivity` hang off the overflow menu.

## Data Privacy & Signing

- **Weight entries are health data.** They must never leave the device unnoticed: `data_extraction_rules.xml` excludes the database and the shared prefs from Google cloud backup (device-to-device transfer stays enabled — it does not go through Google's servers). Keep any new persisted data under these exclusions. The CSV export is the one deliberate way out, and only because the user picks the target document explicitly.
- **Stable signing key** (shared with the sibling FitnessTrainer): both build types are signed via the `WEIGHTTRACKER_RELEASE_*` properties from the user's `~/.gradle/gradle.properties`, pointing to `~/.android/weighttracker-release.jks`. Keystore and credentials must NEVER be committed — the repository is meant to be public (MIT license). Checkouts without the properties build with the default debug key. A signature change forces an uninstall and takes the database with it.
- **The schema is frozen.** The app runs on the user's device with real data that must not be lost, so `app/schemas/de.steffzilla.weighttracker.data.AppDatabase/1.json` is a released baseline, not a draft: every schema change bumps the version and needs a proper Room `Migration` with a `MigrationTest`. Never `fallbackToDestructiveMigration`; never edit or delete a released version file.

## Architecture & Testability

Follow the official Android Architecture Guide (MVVM / Production-ready architecture).

- **Separation of Concerns**: Strictly separate UI (Activities/Fragments), Presentation Logic (ViewModels), and Data (Repositories/Room DAOs).
- **Dependency Injection (DI)**: Do not hardcode dependencies. Design all classes (ViewModels, Repositories) using **Constructor Injection** so they can be easily mocked in Unit Tests.
- **Thread Management**: UI thread must remain unblocked. Use standard Java concurrency tools or explicit executors for background tasks (e.g. Room database operations).
- **State Management**: ViewModels must expose UI state using observable patterns (like `LiveData` or Java-compatible observable fields) to decouple the UI from business logic.
- **Pure logic packages**: `stats/` (chart and statistics computation) and `backup/` (CSV codec, `ImportPlanner`, all-or-nothing import) are framework-free Java. Put new computation there and unit-test it — keep it out of Views, Activities and DAOs.
- **Dates**: a calendar day is persisted as an epoch day (`long` via `LocalDateConverter`), never a timestamp; "today" is passed into the calculators as a value instead of being read from the clock inside them, so day-dependent logic stays deterministically testable.
- **ViewModel wiring**: a `ViewModelProvider.Factory` builds the repository from `AppDatabase.getInstance(...)` and injects an `Executors.newSingleThreadExecutor()`; the ViewModel runs all DB writes on it. Unit tests inject a direct executor (`Runnable::run`) and mock the repository.

Key paths:
- `app/src/main/java/de/steffzilla/weighttracker/` — all source code, split by package:
  - `data/` — Room entity, DAO, repository, type converter, `AppDatabase`
  - `ui/` — Activities, ViewModels + their `ViewModelProvider.Factory`, adapter, chart view
  - `stats/`, `backup/` — framework-free pure logic, unit-tested
  - `settings/`, `about/` — Settings screen + theme handling, About screen + license catalogue
- `app/src/main/res/` — layouts, themes, XML configs
- `app/src/main/keepRules/rules.keep` — ProGuard/R8 keep rules (add entries here, not inline)

## Charts & Statistics

The statistics/trend screen follows a strict "dumb view + pure calculator" split:

- **No charting library.** Charts are drawn in a custom `View` on a `Canvas`
  (`ui/WeightChartView`). MPAndroidChart (unmaintained since 2019) and Vico
  (Compose-centric) were both rejected against the third-party policy. Do not add a
  charting dependency without re-checking that policy.
- **Pure logic in `stats/`.** All filtering, aggregation and chart-model preparation
  live in framework-free Java (`WeightStatisticsCalculator` → immutable `ChartModel` /
  `WeightStatistics` records). `today` is passed in, never read from the clock inside
  the calculator, so range logic is deterministically unit-testable. Put new
  computation here and unit-test it — keep it out of the View and the Activity.
- **The View only renders.** `WeightChartView.setModel(ChartModel)` takes a fully
  prepared model and draws it. No data access, filtering or formatting decisions in
  `onDraw`. Resolve colors from Material 3 theme attributes (`MaterialColors.getColor`),
  never hardcode them, and size text from `sp` dimens for font scaling.
- **Chart accessibility.** A `Canvas` is invisible to TalkBack, so the Activity sets a
  spoken summary via `setContentDescription` on the chart view (value range, dates,
  net change) whenever the model changes.
- **Y-axis is never zero-based.** Scale to the data's min/max with padding; handle the
  degenerate all-equal/single-point case with a fixed fallback span.

## Testing Strategy

- **Unit Tests (`app/src/test/`)**: Write Unit Tests for all pure Java logic, ViewModels, and Repositories. Use **Mockito** for mocking injected dependencies.
- **Instrumented Tests (`app/src/androidTest/`)**: Use Espresso/ActivityScenario only for critical UI flows or Room DAO integration tests that require an Android environment. Prefer Unit Tests wherever possible for speed. Configuration changes are covered via `ActivityScenario.recreate()` (see `AddEditDatePickerRecreateTest`).
- **Test-Driven Mindset**: When implementing a new business rule, ask to write the Unit Test first or simultaneously with the implementation.
- **Lint**: run `./gradlew lintDebug` before every commit, together with the tests. A new finding belongs to the change that caused it — either fix it, or mark it a false positive with a `tools:ignore` plus the reasoning where it applies.
- **Code review**: for a change that touches Java or XML, run `/code-review` over its diff unprompted once tests and `lintDebug` are green and address the findings — propose the commit only afterwards. Doc-only and build-only commits skip this step.

## Naming & Code Language

All **code is English**, the **UI is German** — keep the two strictly apart:

- **English**: every Java identifier (types, fields, methods, params, locals, enum constants), every resource id, every string-resource *name*, and every commit message (see "Commit Messages"). German may appear in code *comments* when naming a domain concept, never as an identifier.
- **German**: this chat and the string-resource *values* (the visible UI text).
- **One concept, one name**: the domain is small enough to need no glossary file — reuse the identifier already in the code (`WeightEntry`, `weight`, `epochDay`, `WeightBounds` for the chart's target band, `stats` for the trend screen) instead of introducing a synonym. A genuinely new domain term gets its English stem agreed with the user before it appears in code.

## Java & Android Style Conventions

- **Modern Java**: Java 21 language level — `switch` expressions, pattern matching and record patterns, `record`s for immutable data transfer objects, sealed types, `var` for locals where it reads better. Language *features* desugar into DEX and are always safe; library *APIs* must exist in Android's core libraries at `minSdk 35`. Let lint's `NewApi` settle doubtful cases.
- **View Binding**: Do not use `findViewById()`. Use **Android View Binding** for XML layouts. Ensure it is enabled in `build.gradle` and properly cleaned up in lifecycles.
- **Material 3**: Use semantic Material 3 color roles (e.g., `?attr/colorOnSurface`) in XML layouts instead of hardcoded hex colors to support Day/Night themes natively.
- **Resources**: Never hardcode strings, dimensions, or colors in Java or XML. Use `strings.xml`, `dimens.xml`, etc. UI language is German.

## Accessibility

The app must be usable with Android's accessibility services (TalkBack, font scaling, display size). Apply these rules consistently:

- **Content descriptions**: Every non-decorative `ImageView`, `ImageButton`, and icon-only button needs `android:contentDescription`. Purely decorative images get `android:importantForAccessibility="no"`.
- **Touch targets**: Interactive elements must be at least 48×48dp.
- **Semantics**: Use semantic widgets (`Button`, `CheckBox`, etc.) rather than click listeners on generic `View`s. Set `android:hint` on all input fields.
- **Color contrast**: Never rely on color alone to convey meaning. Material 3 color roles satisfy contrast requirements by default — do not override them with low-contrast custom values.
- **Font scaling**: Use `sp` for all text sizes so the system font scale is respected. Layouts must not clip or overlap text when the system font size is set to largest.
- **Focus order**: Verify logical focus traversal order in complex layouts. Use `android:nextFocusDown` / `android:nextFocusRight` only when the default order is wrong.

## Dependencies

The authoritative list is `gradle/libs.versions.toml`. Add new dependencies there first, then reference them via `libs.*` aliases in `app/build.gradle`. When a bundled runtime dependency is added, removed, or version-bumped, also update `about/ThirdPartyLibraries` (hand-maintained license list shown on the About screen; test-only dependencies are deliberately absent).

## Third-party Library Policy

Licence, maintenance and adoption are checked before any dependency is added. For this repo:
the app ships under MIT and the repository is public, so only Public Domain, Apache 2.0, MIT,
BSD or similar are acceptable — copyleft (GPL, LGPL, AGPL) is out. Prefer AndroidX, Google,
Square, JetBrains over niche projects; a bundled runtime dependency also needs its entry in
`about/ThirdPartyLibraries`.

## Theme

Base theme is `Theme.Material3.DayNight.NoActionBar`. Light and night variants are in `res/values/themes.xml` and `res/values-night/themes.xml`. The user-selectable theme (System/Hell/Dunkel) is applied in `WeightTrackerApplication` from the `pref_theme` preference before the first Activity is created. EdgeToEdge display is enabled in every Activity — window insets must be applied manually for content that sits under system bars.

## Sibling Project: FitnessTrainer

`../FitnessTrainer` is a second app by the same user, grown out of this one and held to the same standards; it shares the signing keystore. Its repo is the more elaborate one — glossary (`CONTEXT.md`), ADRs (`docs/adr/`) and a written spec — so consult it when a convention here is unclear, and keep patterns that exist in both projects in step.

## Commit Messages

Commit messages are **English**, subject and body. German domain nouns keep their German form (`fix: rename statistics screen title to Gewichtsverlauf`) where they name visible UI text; the prose around them is English.

- **Subject**: `<type>: <what changed>`. Conventional prefixes: `feat:`, `fix:`, `docs:`, `build:`, `refactor:`, `chore:`.
- **Body**: shaped by the global instructions (at most three bullets, no prose); name the key types.
- **No session coordination in the history**: test counts, "not run yet", "after the merge" and similar status notes belong in the report to the user, not in the permanent record — the diff already shows which tests exist.
- Close with the `Co-Authored-By:` trailer when Claude authored the change. Commits the user writes themselves carry no trailer.