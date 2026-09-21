package com.smartshop.features.shop.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.service.BranchService;
import com.smartshop.features.settings.service.SettingsService;
import com.smartshop.features.shop.dto.ShopRequest;
import com.smartshop.features.shop.dto.ShopResponse;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.UserPrincipal;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.ShopStatus;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final SettingsService settingsService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;
    private final BranchService branchService;

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
        branchService.createDefaultMainBranch(saved);

        auditService.log("CREATE", "Shop", saved.getId().toString(), null, saved.getName());
        log.info("Shop created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> listShops(String search, Pageable pageable) {
        UserPrincipal caller = SecurityUtils.currentUser();

        if (caller.hasRole(AppConstants.ROLE_SUPER_ADMIN)) {
            // SUPER_ADMIN: see all shops, optionally filtered by name
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

        // SHOP_ADMIN / STAFF: scope to their own shop(s) derived from JWT grants
        Set<UUID> callerShopIds = caller.getBranchRoleGrants().stream()
                .filter(g -> g.shopId() != null)
                .map(g -> g.shopId())
                .collect(Collectors.toSet());

        if (callerShopIds.isEmpty()) {
            return Page.empty(pageable);
        }

        final String finalTerm = (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";
        Page<Shop> page = shopRepository.findAll((root, query, cb) -> {
            var inOwned = root.get("id").in(callerShopIds);
            if (finalTerm == null) {
                return inOwned;
            }
            return cb.and(inOwned, cb.like(cb.lower(root.get("name")), finalTerm));
        }, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShopResponse getShop(UUID id) {
        // Enforce that the caller has access to this shop.
        // SUPER_ADMIN bypasses inside requireShopAccessOrNotFound; SHOP_ADMIN/STAFF must own it.
        branchScopeGuard.requireShopAccessOrNotFound(SecurityUtils.currentUserId(), id);
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