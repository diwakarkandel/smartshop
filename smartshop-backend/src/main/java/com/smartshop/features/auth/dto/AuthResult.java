package com.smartshop.features.auth.dto;

public record AuthResult(AuthResponse response, String refreshToken) {
}