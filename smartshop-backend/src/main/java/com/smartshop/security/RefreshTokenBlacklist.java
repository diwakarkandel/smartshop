package com.smartshop.security;

import com.smartshop.features.auth.entity.RefreshTokenBlacklistEntity;
import com.smartshop.features.auth.repository.RefreshTokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenBlacklist {

    private final RefreshTokenBlacklistRepository blacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String jti = jwtTokenProvider.getJtiFromToken(token);
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            LocalDateTime expiresAt = expiration != null
                    ? expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : LocalDateTime.now().plusDays(7);

            String key = (jti != null && !jti.isBlank()) ? jti : token;
            if (!blacklistRepository.existsByTokenJti(key)) {
                RefreshTokenBlacklistEntity entity = RefreshTokenBlacklistEntity.builder()
                        .tokenJti(key)
                        .expiresAt(expiresAt)
                        .build();
                blacklistRepository.save(entity);
                log.info("Refresh token blacklisted successfully with identifier: {}", key);
            }
        } catch (Exception e) {
            log.warn("Failed to extract metadata for token blacklisting, saving token directly: {}", e.getMessage());
            if (!blacklistRepository.existsByTokenJti(token)) {
                RefreshTokenBlacklistEntity entity = RefreshTokenBlacklistEntity.builder()
                        .tokenJti(token)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build();
                blacklistRepository.save(entity);
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String jti = jwtTokenProvider.getJtiFromToken(token);
            if (jti != null && blacklistRepository.existsByTokenJti(jti)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return blacklistRepository.existsByTokenJti(token);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running scheduled cleanup of expired blacklisted tokens...");
        long deleted = blacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired blacklisted tokens cleanup complete: {} entries deleted", deleted);
    }
}