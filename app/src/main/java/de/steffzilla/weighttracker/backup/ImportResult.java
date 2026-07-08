package de.steffzilla.weighttracker.backup;

import java.util.List;

/**
 * Outcome of parsing a CSV file: the successfully parsed rows plus the 1-based line
 * numbers that could not be parsed. Import is only attempted when {@link #hasErrors()}
 * is {@code false}.
 */
public record ImportResult(List<ParsedEntry> entries, List<Integer> errorLines) {

    public boolean hasErrors() {
        return !errorLines.isEmpty();
    }
}