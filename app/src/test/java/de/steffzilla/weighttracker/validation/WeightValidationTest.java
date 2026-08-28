package de.steffzilla.weighttracker.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.steffzilla.weighttracker.validation.WeightValidation.Result;

public class WeightValidationTest {

    @Test
    public void plainValue_isValid() {
        assertEquals(Result.VALID, WeightValidation.validate("80.5"));
    }

    @Test
    public void wholeNumber_isValid() {
        assertEquals(Result.VALID, WeightValidation.validate("80"));
    }

    @Test
    public void commaAsDecimalSeparator_isValid() {
        assertEquals(Result.VALID, WeightValidation.validate("80,5"));
    }

    @Test
    public void surroundingWhitespace_isIgnored() {
        assertEquals(Result.VALID, WeightValidation.validate("  80.5  "));
    }

    @Test
    public void nullOrBlank_isEmpty() {
        assertEquals(Result.EMPTY, WeightValidation.validate(null));
        assertEquals(Result.EMPTY, WeightValidation.validate(""));
        assertEquals(Result.EMPTY, WeightValidation.validate("   "));
    }

    @Test
    public void nonNumericText_isNotANumber() {
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("abc"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("8.0.5"));
    }

    @Test
    public void infinityAndNaN_areNotANumber() {
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("Infinity"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("NaN"));
    }

    /**
     * {@code Float.parseFloat} reads these, and the exponent forms would carry more than
     * one decimal past a rule that counts characters — 8005E-2 is 80.05.
     */
    @Test
    public void notationsFloatWouldAccept_areNotANumber() {
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("8005E-2"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("8.005e1"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("80f"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("80d"));
        assertEquals(Result.NOT_A_NUMBER, WeightValidation.validate("0x1p3"));
    }

    @Test
    public void absurdlyLongNumber_isOutOfRangeRatherThanInfinite() {
        assertEquals(Result.OUT_OF_RANGE, WeightValidation.validate("9".repeat(60)));
    }

    @Test
    public void zeroOrNegative_isOutOfRange() {
        assertEquals(Result.OUT_OF_RANGE, WeightValidation.validate("0"));
        assertEquals(Result.OUT_OF_RANGE, WeightValidation.validate("-5"));
    }

    @Test
    public void theUpperBoundItself_isValid() {
        assertEquals(Result.VALID, WeightValidation.validate("999.9"));
    }

    @Test
    public void aboveTheUpperBound_isOutOfRange() {
        assertEquals(Result.OUT_OF_RANGE, WeightValidation.validate("1000"));
    }

    @Test
    public void moreThanOneDecimal_isRejected() {
        assertEquals(Result.TOO_MANY_DECIMALS, WeightValidation.validate("80.55"));
        assertEquals(Result.TOO_MANY_DECIMALS, WeightValidation.validate("80,55"));
    }

    /** Range is judged before the decimals, so the message names the bigger problem. */
    @Test
    public void outOfRangeWithTooManyDecimals_reportsTheRange() {
        assertEquals(Result.OUT_OF_RANGE, WeightValidation.validate("1000.55"));
    }

    @Test
    public void parse_readsWhatValidateAccepted() {
        assertEquals(80.5f, WeightValidation.parse(" 80,5 "), 0.0001f);
    }
}
