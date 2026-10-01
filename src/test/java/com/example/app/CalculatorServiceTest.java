package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CalculatorServiceTest {

    private final CalculatorService calc = new CalculatorService();

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    @Test
    void addIsExactForDecimals() {
        assertEquals("0.3", calc.add(n("0.1"), n("0.2")).toPlainString());
        assertEquals("8", calc.add(n("5"), n("3")).toPlainString());
    }

    @Test
    void subtractHandlesNegativeResults() {
        assertEquals("-5", calc.subtract(n("5"), n("10")).toPlainString());
        assertEquals("0", calc.subtract(n("2.5"), n("2.5")).toPlainString());
    }

    @Test
    void multiplyTrimsTrailingZeros() {
        assertEquals("12.5", calc.multiply(n("2.5"), n("5")).toPlainString());
        assertEquals("100", calc.multiply(n("10"), n("10")).toPlainString());
    }

    @Test
    void divideRoundsToTenPlaces() {
        assertEquals("2", calc.divide(n("10"), n("5")).toPlainString());
        assertEquals("0.3333333333", calc.divide(n("1"), n("3")).toPlainString());
    }

    @Test
    void divideByZeroIsRejected() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> calc.divide(n("1"), n("0")));
        assertEquals(CalculatorService.MSG_DIVIDE_BY_ZERO, ex.getMessage());
    }

    @Test
    void sqrtOfPerfectSquareAndIrrational() {
        assertEquals("12", calc.sqrt(n("144")).toPlainString());
        assertEquals("0", calc.sqrt(n("0")).toPlainString());
        assertTrue(calc.sqrt(n("2")).toPlainString().startsWith("1.41421356237"));
    }

    @Test
    void sqrtOfNegativeIsRejected() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> calc.sqrt(n("-4")));
        assertEquals(CalculatorService.MSG_NEGATIVE_SQRT, ex.getMessage());
    }

    @Test
    void percentageModes() {
        assertEquals("20", calc.percentOf(n("25"), n("80")).toPlainString());
        assertEquals("25", calc.percentChange(n("80"), n("100")).toPlainString());
        assertEquals("-50", calc.percentChange(n("200"), n("100")).toPlainString());
        assertEquals("25", calc.whatPercent(n("20"), n("80")).toPlainString());
    }

    @Test
    void percentageDivisionByZeroIsRejected() {
        assertThrows(ArithmeticException.class, () -> calc.percentChange(n("0"), n("5")));
        assertThrows(ArithmeticException.class, () -> calc.whatPercent(n("5"), n("0")));
    }

    @Test
    void parseAcceptsPlainNumbers() {
        assertEquals("42", CalculatorService.parse(" 42 ").toPlainString());
        assertEquals("-3.5", CalculatorService.parse("-3.5").toPlainString());
    }

    @Test
    void powerWorks() {
        assertEquals("8", calc.power(n("2"), n("3")).toPlainString());
        assertEquals("1", calc.power(n("5"), n("0")).toPlainString());
        assertThrows(IllegalArgumentException.class, () -> calc.power(n("2"), n("-1")));
    }

    @Test
    void parseRejectsBadInput() {
        for (String bad : new String[] { null, "", "  ", "abc", "1e5", "1,5", "NaN", "Infinity", "--1", "1.", ".5",
                "1".repeat(41) }) {
            Executable call = () -> CalculatorService.parse(bad);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call, "input: " + bad);
            assertEquals(CalculatorService.MSG_INVALID_NUMBER, ex.getMessage());
        }
    }
}
