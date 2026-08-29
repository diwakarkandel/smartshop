package com.smartshop.features.settings.repository;

import com.smartshop.features.settings.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettingRepository extends JpaRepository<Setting, UUID> {

    Optional<Setting> findByShopIdAndSettingKey(UUID shopId, String key);

    List<Setting> findByShopIdOrderBySettingKeyAsc(UUID shopId);

    boolean existsByShopIdAndSettingKey(UUID shopId, String key);
}