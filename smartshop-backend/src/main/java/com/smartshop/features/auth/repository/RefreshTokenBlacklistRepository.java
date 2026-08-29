package com.smartshop.features.auth.repository;

import com.smartshop.features.auth.entity.RefreshTokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenBlacklistRepository extends JpaRepository<RefreshTokenBlacklistEntity, UUID> {

    Optional<RefreshTokenBlacklistEntity> findByTokenJti(String tokenJti);

    boolean existsByTokenJti(String tokenJti);

    @Modifying
    @Query("DELETE FROM RefreshTokenBlacklistEntity b WHERE b.expiresAt < :now")
    long deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
