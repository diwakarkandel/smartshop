package com.smartshop.shared.exception;

import java.math.BigDecimal;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String productName, BigDecimal requested, BigDecimal available) {
        super("Insufficient stock for " + productName + ": requested " + requested.toPlainString()
                + ", available " + available.toPlainString());
    }
}