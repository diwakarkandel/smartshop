package com.smartshop.features.shop.repository;

import com.smartshop.features.shop.entity.ShopInviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopInviteCodeRepository extends JpaRepository<ShopInviteCode, UUID> {

    Optional<ShopInviteCode> findFirstByShopIdAndIsActiveTrueOrderByIdDesc(UUID shopId);

    List<ShopInviteCode> findByShopIdAndIsActiveTrue(UUID shopId);

    Optional<ShopInviteCode> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);
}
