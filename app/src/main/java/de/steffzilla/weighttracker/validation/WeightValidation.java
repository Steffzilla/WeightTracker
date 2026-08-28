package de.steffzilla.weighttracker.validation;

import java.util.regex.Pattern;

/**
 * The single rule for what counts as a weight entry the app will store, shared by the
 * entry form and the CSV import so a file cannot introduce values the form would reject.
 * Framework-free, so both callers can be unit-tested against it. The chart's target band
 * ({@code WeightBounds}) is deliberately looser — it is a goal, not a measurement.
 *
 * <p>Validation works on the raw text rather than a parsed {@code float}, because the
 * "at most one decimal place" rule cannot be read back off a float — {@code 80.3f} is
 * really 80.30000305, and every parsed value would fail it.
 */
public final class WeightValidation {

    /** Heaviest weight the app accepts, in kilograms. */
    public static final float MAX_WEIGHT_KG = 999.9f;

    /**
     * Plain decimal notation, the only form accepted. {@link Float#parseFloat} is far more
     * permissive — it reads {@code 8005E-2}, {@code 80f} and {@code Infinity} — and the
     * exponent forms would slip past the decimal-place rule, which counts characters.
     */
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

    /** Why an input was rejected, or {@link #VALID} if it was not. */
    public enum Result {
        VALID,
        EMPTY,
        NOT_A_NUMBER,
        OUT_OF_RANGE,
        TOO_MANY_DECIMALS
    }

    private WeightValidation() {
    }

    /**
     * Checks raw input from the entry form or a CSV field. Surrounding whitespace is
     * ignored and a comma is accepted as the decimal separator, which the entry form needs
     * on a German keyboard; the export format writes a dot and passes the same rule.
     */
    public static Result validate(String raw) {
        if (raw == null) {
            return Result.EMPTY;
        }
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return Result.EMPTY;
        }
        if (!PLAIN_DECIMAL.matcher(normalized).matches()) {
            return Result.NOT_A_NUMBER;
        }

        // Overflows to Infinity for absurdly long input, which the range check rejects.
        float value = Float.parseFloat(normalized);
        if (value <= 0f || value > MAX_WEIGHT_KG) {
            return Result.OUT_OF_RANGE;
        }

        int dotIndex = normalized.indexOf('.');
        if (dotIndex != -1 && normalized.length() - dotIndex - 1 > 1) {
            return Result.TOO_MANY_DECIMALS;
        }
        return Result.VALID;
    }

    /**
     * Parses input that {@link #validate(String)} returned {@link Result#VALID} for.
     *
     * @throws NumberFormatException if it did not
     */
    public static float parse(String raw) {
        return Float.parseFloat(normalize(raw));
    }

    private static String normalize(String raw) {
        return raw.trim().replace(',', '.');
    }
}
