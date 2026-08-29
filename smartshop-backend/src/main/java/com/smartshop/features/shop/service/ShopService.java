package com.smartshop.features.shop.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.settings.service.SettingsService;
import com.smartshop.features.shop.dto.ShopRequest;
import com.smartshop.features.shop.dto.ShopResponse;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.ShopStatus;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final SettingsService settingsService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    @Transactional
    public ShopResponse createShop(ShopRequest request) {
        log.info("Creating shop with name: {}", request.getName());
        branchScopeGuard.requireSuperAdmin(SecurityUtils.currentUserId());
        if (shopRepository.existsByPanVatNumber(request.getPanVatNumber())) {
            throw new DuplicateResourceException("A shop with PAN/VAT number " + request.getPanVatNumber() + " already exists");
        }
        Shop shop = new Shop();
        applyRequest(shop, request);
        shop.setStatus(request.getStatus() == null ? ShopStatus.ACTIVE : request.getStatus());
        Shop saved = shopRepository.save(shop);
        settingsService.ensureDefaultSettings(saved, SecurityUtils.currentUserId());
        
        auditService.log("CREATE", "Shop", saved.getId().toString(), null, saved.getName());
        log.info("Shop created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> listShops(String search, Pageable pageable) {
        Page<Shop> page;
        if (search == null || search.isBlank()) {
            page = shopRepository.findAll(pageable);
        } else {
            page = shopRepository.findAll(
                    (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShopResponse getShop(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ShopResponse updateShop(UUID id, ShopRequest request) {
        log.info("Updating shop: {}", id);
        branchScopeGuard.requireSuperAdmin(SecurityUtils.currentUserId());
        Shop shop = getEntity(id);
        if (!shop.getPanVatNumber().equalsIgnoreCase(request.getPanVatNumber())
                && shopRepository.existsByPanVatNumber(request.getPanVatNumber())) {
            throw new DuplicateResourceException("A shop with PAN/VAT number " + request.getPanVatNumber() + " already exists");
        }
        String oldName = shop.getName();
        applyRequest(shop, request);
        if (request.getStatus() != null) {
            shop.setStatus(request.getStatus());
        }
        Shop saved = shopRepository.save(shop);
        auditService.log("UPDATE", "Shop", saved.getId().toString(), oldName, saved.getName());
        log.info("Shop {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void deleteShop(UUID id) {
        log.info("Deactivating shop: {}", id);
        branchScopeGuard.requireSuperAdmin(SecurityUtils.currentUserId());
        Shop shop = getEntity(id);
        shop.setStatus(ShopStatus.INACTIVE);
        shopRepository.save(shop);
        auditService.log("DELETE", "Shop", shop.getId().toString(), shop.getName(), "INACTIVE");
        log.info("Shop {} deactivated", id);
    }

    public Shop getEntity(UUID id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", id));
    }

    private void applyRequest(Shop shop, ShopRequest request) {
        shop.setName(request.getName());
        shop.setPanVatNumber(request.getPanVatNumber());
        shop.setPhone(request.getPhone());
        shop.setEmail(request.getEmail());
        shop.setAddress(request.getAddress());
        shop.setLogoUrl(request.getLogoUrl());
    }

    private ShopResponse toResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .panVatNumber(shop.getPanVatNumber())
                .phone(shop.getPhone())
                .email(shop.getEmail())
                .address(shop.getAddress())
                .logoUrl(shop.getLogoUrl())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .build();
    }
}