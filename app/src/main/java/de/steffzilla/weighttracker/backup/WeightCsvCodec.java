package de.steffzilla.weighttracker.backup;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.steffzilla.weighttracker.data.WeightEntry;

/**
 * Pure, framework-free CSV serialization and parsing for weight entries. No Android or
 * Room dependencies, so the format is fully unit-testable.
 *
 * <p>The on-disk format is a single header line ({@link #HEADER}) followed by one
 * {@code yyyy-MM-dd,<weight>} row per entry, sorted by date descending (newest first).
 * Dates use ISO-8601 and weights use a locale-independent {@code '.'} decimal separator
 * via {@link Float#toString(float)}, so the file round-trips exactly regardless of the
 * device locale.
 */
public final class WeightCsvCodec {

    /** Header written as the first line and tolerated (skipped) when reading. */
    public static final String HEADER = "date,weight_kg";

    private static final String SEPARATOR = ",";
    private static final String NEWLINE = "\n";

    /** Serializes entries to CSV, sorted by date descending (newest first). */
    public String encode(List<WeightEntry> entries) {
        List<WeightEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(WeightEntry::getDate).reversed());

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(NEWLINE);
        for (WeightEntry entry : sorted) {
            sb.append(entry.getDate().toString())
                    .append(SEPARATOR)
                    .append(Float.toString(entry.getWeightKg()))
                    .append(NEWLINE);
        }
        return sb.toString();
    }

    /**
     * Parses CSV content. A leading header line is skipped if present and blank lines are
     * ignored. Any line that is not exactly a valid ISO date and a positive, finite weight
     * — and any date that appears more than once — is reported as an error via its 1-based
     * line number. Callers must not import when {@link ImportResult#hasErrors()} is true.
     */
    public ImportResult decode(String content) {
        List<ParsedEntry> entries = new ArrayList<>();
        List<Integer> errorLines = new ArrayList<>();
        Set<LocalDate> seenDates = new HashSet<>();

        String[] lines = content.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;

            if (line.isEmpty()) {
                continue;
            }
            if (i == 0 && line.equalsIgnoreCase(HEADER)) {
                continue;
            }

            String[] fields = line.split(SEPARATOR, -1);
            if (fields.length != 2) {
                errorLines.add(lineNumber);
                continue;
            }

            LocalDate date;
            float weightKg;
            try {
                date = LocalDate.parse(fields[0].trim());
                weightKg = Float.parseFloat(fields[1].trim());
            } catch (DateTimeParseException | NumberFormatException ex) {
                errorLines.add(lineNumber);
                continue;
            }
            if (!Float.isFinite(weightKg) || weightKg <= 0f) {
                errorLines.add(lineNumber);
                continue;
            }
            if (!seenDates.add(date)) {
                errorLines.add(lineNumber); // duplicate date within the file
                continue;
            }
            entries.add(new ParsedEntry(date, weightKg));
        }
        return new ImportResult(entries, errorLines);
    }
}