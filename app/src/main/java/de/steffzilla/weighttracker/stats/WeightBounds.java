package de.steffzilla.weighttracker.stats;

/**
 * The user-configured target band drawn on the chart: an optional lower and upper
 * weight limit in kg. Either side may be {@code null} (not set). Kept framework-free so
 * it can be fed into {@link WeightStatisticsCalculator} like {@code today} and parsed
 * deterministically in unit tests.
 */
public record WeightBounds(Float lowerKg, Float upperKg) {

    /** SharedPreferences keys, shared by {@code preferences.xml} and the reading glue. */
    public static final String PREF_KEY_LOWER = "pref_weight_lower";
    public static final String PREF_KEY_UPPER = "pref_weight_upper";

    /** Accepted range, mirroring the weight-entry validation (0,1 .. 999,9 kg). */
    private static final float MIN_WEIGHT = 0.1f;
    private static final float MAX_WEIGHT = 999.9f;

    public static final WeightBounds NONE = new WeightBounds(null, null);

    public boolean hasLower() {
        return lowerKg != null;
    }

    public boolean hasUpper() {
        return upperKg != null;
    }

    public boolean isEmpty() {
        return lowerKg == null && upperKg == null;
    }

    /**
     * Parses two raw preference strings into bounds. Blank, unparseable, or
     * out-of-range values become {@code null} (unset). A German decimal comma is
     * accepted. The two sides are parsed independently; no ordering is enforced.
     */
    public static WeightBounds fromPreferences(String lower, String upper) {
        return new WeightBounds(parse(lower), parse(upper));
    }

    private static Float parse(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            return null;
        }
        float value;
        try {
            value = Float.parseFloat(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
        if (value < MIN_WEIGHT || value > MAX_WEIGHT) {
            return null;
        }
        return value;
    }
}