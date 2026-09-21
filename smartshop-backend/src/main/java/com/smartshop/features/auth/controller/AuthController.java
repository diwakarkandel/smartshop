package com.smartshop.features.auth.controller;

import com.smartshop.features.auth.dto.AuthResult;
import com.smartshop.features.auth.dto.LoginRequest;
import com.smartshop.features.auth.dto.RegisterRequest;
import com.smartshop.features.auth.service.AuthService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    /** Defaults to true (HTTPS-only cookie). Set app.cookie.secure=false in .env for local HTTP dev. */
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /** Comma-separated trusted proxy IPs — same list used by LoginRateLimitFilter. */
    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesConfig;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest request,
                                                        HttpServletResponse response) {
        AuthResult result = authService.register(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", result.response()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest servletRequest,
                                                     HttpServletResponse response) {
        AuthResult result = authService.login(request, resolveIp(servletRequest));
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Login successful", result.response()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Issue a new access token using the refresh token")
    public ResponseEntity<ApiResponse<Object>> refresh(HttpServletRequest request,
                                                       HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", result.response()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link/token")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.smartshop.features.auth.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with this email, password reset instructions have been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.smartshop.features.auth.dto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. You may now log in.", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email with token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody(required = false) com.smartshop.features.auth.dto.VerifyEmailRequest body,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String token) {
        String effectiveToken = token;
        if ((effectiveToken == null || effectiveToken.isBlank()) && body != null) {
            effectiveToken = body.getToken();
        }
        if (effectiveToken == null || effectiveToken.isBlank()) {
            throw new com.smartshop.shared.exception.BadRequestException("Verification token is required");
        }
        authService.verifyEmail(effectiveToken);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification token")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@org.springframework.web.bind.annotation.RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // Only honour X-Forwarded-For when the direct TCP peer is a trusted proxy.
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            boolean fromTrustedProxy = java.util.Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .anyMatch(remoteAddr::equals);
            if (fromTrustedProxy) {
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
            }
        }
        return remoteAddr;
    }
}