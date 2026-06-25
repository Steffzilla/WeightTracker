package de.steffzilla.weighttracker.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeightBoundsTest {

    private static final float DELTA = 0.0001f;

    @Test
    public void none_isEmpty() {
        assertTrue(WeightBounds.NONE.isEmpty());
        assertFalse(WeightBounds.NONE.hasLower());
        assertFalse(WeightBounds.NONE.hasUpper());
    }

    @Test
    public void fromPreferences_parsesBothSides() {
        WeightBounds bounds = WeightBounds.fromPreferences("70", "85");

        assertTrue(bounds.hasLower());
        assertTrue(bounds.hasUpper());
        assertEquals(70f, bounds.lowerKg(), DELTA);
        assertEquals(85f, bounds.upperKg(), DELTA);
        assertFalse(bounds.isEmpty());
    }

    @Test
    public void fromPreferences_acceptsGermanDecimalComma() {
        WeightBounds bounds = WeightBounds.fromPreferences("72,5", "84,9");

        assertEquals(72.5f, bounds.lowerKg(), DELTA);
        assertEquals(84.9f, bounds.upperKg(), DELTA);
    }

    @Test
    public void fromPreferences_treatsBlankAndNullAsUnset() {
        WeightBounds bounds = WeightBounds.fromPreferences(null, "   ");

        assertNull(bounds.lowerKg());
        assertNull(bounds.upperKg());
        assertTrue(bounds.isEmpty());
    }

    @Test
    public void fromPreferences_treatsGarbageAsUnset() {
        WeightBounds bounds = WeightBounds.fromPreferences("abc", "8o");

        assertNull(bounds.lowerKg());
        assertNull(bounds.upperKg());
    }

    @Test
    public void fromPreferences_rejectsOutOfRangeValues() {
        WeightBounds tooLow = WeightBounds.fromPreferences("0", "1000");
        assertNull(tooLow.lowerKg());
        assertNull(tooLow.upperKg());

        WeightBounds negative = WeightBounds.fromPreferences("-5", "");
        assertNull(negative.lowerKg());
    }

    @Test
    public void fromPreferences_keepsValuesAtRangeEdges() {
        WeightBounds bounds = WeightBounds.fromPreferences("0,1", "999,9");

        assertEquals(0.1f, bounds.lowerKg(), DELTA);
        assertEquals(999.9f, bounds.upperKg(), DELTA);
    }

    @Test
    public void fromPreferences_parsesSidesIndependently() {
        WeightBounds onlyUpper = WeightBounds.fromPreferences("", "82");

        assertFalse(onlyUpper.hasLower());
        assertTrue(onlyUpper.hasUpper());
        assertEquals(82f, onlyUpper.upperKg(), DELTA);
    }
}