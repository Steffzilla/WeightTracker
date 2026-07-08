package de.steffzilla.weighttracker.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.steffzilla.weighttracker.data.WeightEntry;

public class WeightCsvCodecTest {

    private final WeightCsvCodec codec = new WeightCsvCodec();

    @After
    public void resetLocale() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void encode_writesHeaderAndSortsDescendingByDate() {
        WeightEntry older = new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f);
        WeightEntry newer = new WeightEntry(LocalDate.of(2026, 3, 1), 79.5f);

        String csv = codec.encode(Arrays.asList(older, newer));

        String[] lines = csv.split("\n");
        assertEquals(WeightCsvCodec.HEADER, lines[0]);
        assertEquals("2026-03-01,79.5", lines[1]);
        assertEquals("2026-01-01,80.0", lines[2]);
    }

    @Test
    public void encode_usesDotDecimalSeparatorRegardlessOfLocale() {
        Locale.setDefault(Locale.GERMANY);
        WeightEntry entry = new WeightEntry(LocalDate.of(2026, 5, 4), 82.4f);

        String csv = codec.encode(List.of(entry));

        assertTrue(csv.contains("2026-05-04,82.4"));
    }

    @Test
    public void decode_parsesValidRowsWithHeader() {
        String csv = "date,weight_kg\n2026-03-01,79.5\n2026-01-01,80.0\n";

        ImportResult result = codec.decode(csv);

        assertFalse(result.hasErrors());
        assertEquals(2, result.entries().size());
        assertEquals(LocalDate.of(2026, 3, 1), result.entries().get(0).date());
        assertEquals(79.5f, result.entries().get(0).weightKg(), 0.0001f);
    }

    @Test
    public void decode_parsesRowsWithoutHeader() {
        ImportResult result = codec.decode("2026-01-01,80.0\n");

        assertFalse(result.hasErrors());
        assertEquals(1, result.entries().size());
    }

    @Test
    public void decode_ignoresBlankLines() {
        ImportResult result = codec.decode("date,weight_kg\n\n2026-01-01,80.0\n\n");

        assertFalse(result.hasErrors());
        assertEquals(1, result.entries().size());
    }

    @Test
    public void decode_reportsMalformedLineNumbers() {
        // Line 1 header, line 2 valid, line 3 bad date, line 4 non-numeric weight, line 5 wrong field count
        String csv = "date,weight_kg\n2026-01-01,80.0\n2026-13-99,70\nfoo,bar\n2026-02-01\n";

        ImportResult result = codec.decode(csv);

        assertTrue(result.hasErrors());
        assertEquals(Arrays.asList(3, 4, 5), result.errorLines());
        assertEquals(1, result.entries().size());
    }

    @Test
    public void decode_treatsDuplicateDateInFileAsError() {
        String csv = "2026-01-01,80.0\n2026-01-01,81.0\n";

        ImportResult result = codec.decode(csv);

        assertTrue(result.hasErrors());
        assertEquals(List.of(2), result.errorLines());
    }

    @Test
    public void decode_rejectsNonPositiveWeight() {
        ImportResult result = codec.decode("2026-01-01,0\n2026-01-02,-5\n");

        assertEquals(Arrays.asList(1, 2), result.errorLines());
        assertTrue(result.entries().isEmpty());
    }

    @Test
    public void decode_handlesCarriageReturnLineEndings() {
        ImportResult result = codec.decode("date,weight_kg\r\n2026-01-01,80.0\r\n");

        assertFalse(result.hasErrors());
        assertEquals(1, result.entries().size());
    }

    @Test
    public void encodeThenDecode_roundTripsExactly() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(LocalDate.of(2026, 1, 1), 80.0f),
                new WeightEntry(LocalDate.of(2026, 2, 1), 79.3f));

        ImportResult result = codec.decode(codec.encode(entries));

        assertFalse(result.hasErrors());
        assertEquals(2, result.entries().size());
        // Encoded newest-first: 2026-02-01 then 2026-01-01
        assertEquals(LocalDate.of(2026, 2, 1), result.entries().get(0).date());
        assertEquals(79.3f, result.entries().get(0).weightKg(), 0.0001f);
        assertEquals(80.0f, result.entries().get(1).weightKg(), 0.0001f);
    }
}