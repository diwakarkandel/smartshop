package com.smartshop.shared.exception;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(message);
    }
}