package com.example.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Pure arithmetic logic. No HTTP, no I/O, which keeps it trivial to unit test.
 *
 * <p>
 * All maths uses {@link BigDecimal} so that results are exact
 * (for example 0.1 + 0.2 is 0.3, not 0.30000000000000004).
 */
public class CalculatorService {

    public static final String MSG_INVALID_NUMBER = "Please provide valid numbers for this operation.";
    public static final String MSG_DIVIDE_BY_ZERO = "Division by zero is undefined.";
    public static final String MSG_NEGATIVE_SQRT = "Square root of a negative number is not real.";

    private static final int DIVISION_SCALE = 10;
    private static final MathContext SQRT_CONTEXT = new MathContext(15, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Plain decimal numbers only (no exponents), at most 40 characters. */
    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final int MAX_INPUT_LENGTH = 40;

    /**
     * Parses user input into a number or throws {@link IllegalArgumentException}.
     */
    public static BigDecimal parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException(MSG_INVALID_NUMBER);
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_INPUT_LENGTH || !NUMBER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(MSG_INVALID_NUMBER);
        }
        return new BigDecimal(trimmed);
    }

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return clean(a.add(b));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return clean(a.subtract(b));
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return clean(a.multiply(b));
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        requireNonZero(b);
        return clean(a.divide(b, DIVISION_SCALE, RoundingMode.HALF_UP));
    }

    public BigDecimal sqrt(BigDecimal a) {
        if (a.signum() < 0) {
            throw new ArithmeticException(MSG_NEGATIVE_SQRT);
        }
        return clean(a.sqrt(SQRT_CONTEXT));
    }

    /** {@code percent}% of {@code value}, for example 25% of 80 = 20. */
    public BigDecimal percentOf(BigDecimal percent, BigDecimal value) {
        return clean(percent.multiply(value).divide(HUNDRED, DIVISION_SCALE, RoundingMode.HALF_UP));
    }

    /** Percentage change from {@code oldValue} to {@code newValue}. */
    public BigDecimal percentChange(BigDecimal oldValue, BigDecimal newValue) {
        requireNonZero(oldValue);
        return clean(newValue.subtract(oldValue).multiply(HUNDRED)
                .divide(oldValue, DIVISION_SCALE, RoundingMode.HALF_UP));
    }

    /** What percentage {@code part} is of {@code whole}. */
    public BigDecimal whatPercent(BigDecimal part, BigDecimal whole) {
        requireNonZero(whole);
        return clean(part.multiply(HUNDRED).divide(whole, DIVISION_SCALE, RoundingMode.HALF_UP));
    }

    /** Raises base to a whole-number exponent between 0 and 100. */
    public BigDecimal power(BigDecimal base, BigDecimal exponent) {
        int exp;
        try {
            exp = exponent.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Exponent must be a whole number between 0 and 100.");
        }
        if (exp < 0 || exp > 100) {
            throw new IllegalArgumentException("Exponent must be a whole number between 0 and 100.");
        }
        return clean(base.pow(exp));
    }

    private static void requireNonZero(BigDecimal value) {
        if (value.signum() == 0) {
            throw new ArithmeticException(MSG_DIVIDE_BY_ZERO);
        }
    }

    /** Removes trailing zeros so 12.5000 becomes 12.5 and 0.000 becomes 0. */
    static BigDecimal clean(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}
