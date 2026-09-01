package com.finanzas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private Money() {
    }

    public static BigDecimal of(double value) {
        return normalize(BigDecimal.valueOf(value));
    }

    public static BigDecimal of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ZERO;
        }
        return normalize(new BigDecimal(value.trim()));
    }

    public static BigDecimal parseFlexible(String value) {
        String sanitized = value == null ? "" : value.trim();
        sanitized = sanitized.replace("$", "").replace(" ", "");
        if (sanitized.isEmpty()) {
            throw new NumberFormatException("Monto vacio");
        }
        if (sanitized.contains(",") && sanitized.contains(".")) {
            sanitized = sanitized.replace(".", "").replace(",", ".");
        } else if (sanitized.contains(",")) {
            sanitized = sanitized.replace(",", ".");
        } else if (sanitized.indexOf('.') != sanitized.lastIndexOf('.')) {
            sanitized = sanitized.replace(".", "");
        }
        return normalize(new BigDecimal(sanitized));
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(SCALE, ROUNDING);
    }

    public static double toDouble(BigDecimal value) {
        return normalize(value).doubleValue();
    }
}
