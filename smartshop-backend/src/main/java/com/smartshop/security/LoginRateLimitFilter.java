package com.smartshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.shared.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    /**
     * Comma-separated list of trusted reverse-proxy CIDRs/IPs whose
     * X-Forwarded-For header we will honour.
     * Example in .env:  APP_TRUSTED_PROXIES=10.0.0.1,10.0.0.2
     * Leave blank (default) to NEVER trust X-Forwarded-For — safest for local dev.
     */
    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesConfig;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equalsIgnoreCase("POST")
                || !request.getRequestURI().equals("/api/v1/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        if (!rateLimiterService.isAllowed(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Too many login attempts. Please try again later.")));
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Returns the real client IP.
     * X-Forwarded-For is ONLY trusted when the direct TCP peer (remoteAddr) is
     * in the configured trusted-proxy list — preventing header spoofing.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        Set<String> trustedProxies = parseTrustedProxies();
        if (!trustedProxies.isEmpty() && trustedProxies.contains(remoteAddr)) {
            // Request came from a known proxy — use the first IP in X-Forwarded-For
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }

        // Either no trusted proxies configured, or request did not come from one.
        // Always fall back to the actual TCP peer address.
        return remoteAddr;
    }

    private Set<String> parseTrustedProxies() {
        if (trustedProxiesConfig == null || trustedProxiesConfig.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxiesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}