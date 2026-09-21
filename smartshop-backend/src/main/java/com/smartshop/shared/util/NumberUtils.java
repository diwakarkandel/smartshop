package com.smartshop.shared.util;

import java.math.BigDecimal;

/**
 * Numeric coercion helpers shared across services that read native-query
 * {@code Object[]} projections (where a column may arrive as {@link BigDecimal},
 * some other {@link Number}, or {@code null}).
 *
 * <p>Extracted from the previously duplicated private {@code toBigDecimal}
 * methods in the pricing, recommendation and report services (DRY / single
 * source of truth).
 */
public final class NumberUtils {

    private NumberUtils() {
    }

    /**
     * Coerces an arbitrary value to a {@link BigDecimal}, defaulting to
     * {@link BigDecimal#ZERO} for {@code null} or non-numeric input.
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return BigDecimal.ZERO;
    }
}
