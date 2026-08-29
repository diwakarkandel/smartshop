package com.smartshop.features.settings.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.settings.entity.Setting;
import com.smartshop.features.settings.repository.SettingRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingRepository settingRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public BigDecimal getVatRate(UUID shopId) {
        Setting setting = settingRepository.findByShopIdAndSettingKey(shopId, AppConstants.SETTING_VAT_RATE)
                .orElse(null);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return new BigDecimal(AppConstants.DEFAULT_VAT_RATE);
        }
        return new BigDecimal(setting.getSettingValue());
    }

    @Transactional
    public void ensureDefaultSettings(Shop shop, UUID updatedBy) {
        if (!settingRepository.existsByShopIdAndSettingKey(shop.getId(), AppConstants.SETTING_VAT_RATE)) {
            log.info("Creating default settings for shop {}", shop.getId());
            Setting vat = new Setting();
            vat.setShop(shop);
            vat.setSettingKey(AppConstants.SETTING_VAT_RATE);
            vat.setSettingValue(AppConstants.DEFAULT_VAT_RATE);
            vat.setDescription("VAT rate percentage applied to sales and purchases");
            settingRepository.save(vat);
        }
    }

    @Transactional
    public Setting updateSetting(UUID shopId, String key, String value, UUID updatedBy) {
        log.info("Updating setting {} for shop {}", key, shopId);
        Setting setting = settingRepository.findByShopIdAndSettingKey(shopId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", key));
        String oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        Setting saved = settingRepository.save(setting);

        auditService.log("UPDATE", "Settings", saved.getId().toString(),
                key + "=" + oldValue, key + "=" + value);
        log.info("Setting {} updated successfully for shop {}", key, shopId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Setting getSetting(UUID shopId, String key) {
        return settingRepository.findByShopIdAndSettingKey(shopId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", key));
    }

    @Transactional(readOnly = true)
    public List<Setting> listSettings(UUID shopId) {
        return settingRepository.findByShopIdOrderBySettingKeyAsc(shopId);
    }
}