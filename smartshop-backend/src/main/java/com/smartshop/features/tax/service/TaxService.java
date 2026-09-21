package com.smartshop.features.tax.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.tax.dto.*;
import com.smartshop.features.tax.entity.Tax;
import com.smartshop.features.tax.entity.TaxRate;
import com.smartshop.features.tax.repository.TaxRateRepository;
import com.smartshop.features.tax.repository.TaxRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.ShopAdminGuard;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxRepository taxRepository;
    private final TaxRateRepository taxRateRepository;
    private final ShopRepository shopRepository;
    private final ShopAdminGuard shopAdminGuard;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public TaxResponse create(TaxRequest request) {
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));

        if (taxRepository.existsByShopIdAndNameAndIdNot(shop.getId(), request.getName(), UUID.randomUUID())) {
            throw new BadRequestException("A tax with name '" + request.getName() + "' already exists for this shop");
        }

        Tax tax = Tax.builder()
                .shop(shop)
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        taxRepository.save(tax);
        auditService.log("CREATE", "Tax", tax.getId().toString(), null,
                tax.getName() + " (" + tax.getType() + ")");
        log.info("Created tax '{}' for shop {}", tax.getName(), shop.getId());

        if (request.getRates() != null) {
            for (TaxRateRequest rr : request.getRates()) {
                saveTaxRate(tax, rr);
            }
        }
        return toResponse(tax);
    }

    @Transactional(readOnly = true)
    public TaxResponse get(UUID taxId) {
        Tax tax = findTax(taxId);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), tax.getShop().getId());
        return toResponse(tax);
    }

    @Transactional(readOnly = true)
    public List<TaxResponse> listByShop(UUID shopId, boolean activeOnly) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        List<Tax> taxes = activeOnly
                ? taxRepository.findByShopIdAndIsActiveTrue(shopId)
                : taxRepository.findByShopId(shopId);
        return taxes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaxResponse update(UUID taxId, TaxRequest request) {
        Tax tax = findTax(taxId);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), tax.getShop().getId());

        // Check name uniqueness (exclude current record)
        if (taxRepository.existsByShopIdAndNameAndIdNot(tax.getShop().getId(), request.getName(), taxId)) {
            throw new BadRequestException("A tax with name '" + request.getName() + "' already exists for this shop");
        }

        String oldValue = tax.getName() + " (" + tax.getType() + ", active=" + tax.getIsActive() + ")";
        tax.setName(request.getName());
        tax.setType(request.getType());
        tax.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            tax.setIsActive(request.getIsActive());
        }
        taxRepository.save(tax);
        auditService.log("UPDATE", "Tax", tax.getId().toString(), oldValue,
                tax.getName() + " (" + tax.getType() + ", active=" + tax.getIsActive() + ")");
        log.info("Updated tax '{}' (id={})", tax.getName(), taxId);
        return toResponse(tax);
    }

    @Transactional
    public void delete(UUID taxId) {
        Tax tax = findTax(taxId);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), tax.getShop().getId());
        auditService.log("DELETE", "Tax", tax.getId().toString(),
                tax.getName() + " (" + tax.getType() + ")", null);
        taxRepository.delete(tax);
        log.info("Deleted tax id={}", taxId);
    }

    // ─── TAX RATE MANAGEMENT ──────────────────────────────────────────────────

    @Transactional
    public TaxRateResponse addRate(UUID taxId, TaxRateRequest request) {
        Tax tax = findTax(taxId);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), tax.getShop().getId());
        if (request.getValidTo() != null && request.getValidTo().isBefore(request.getValidFrom())) {
            throw new BadRequestException("validTo must be on or after validFrom");
        }
        TaxRate rate = saveTaxRate(tax, request);
        auditService.log("ADD_RATE", "Tax", tax.getId().toString(), null,
                "rate=" + rate.getRate() + "% from " + rate.getValidFrom());
        log.info("Added tax rate {} for tax {} effective {}", rate.getRate(), taxId, rate.getValidFrom());
        return toRateResponse(rate);
    }

    @Transactional(readOnly = true)
    public List<TaxRateResponse> getRates(UUID taxId) {
        Tax tax = findTax(taxId);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), tax.getShop().getId());
        return taxRateRepository.findByTaxIdOrderByValidFromDesc(taxId).stream()
                .map(this::toRateResponse).toList();
    }

    // ─── INTERNAL RATE RESOLUTION (used by SaleService / PurchaseService) ────

    /**
     * Returns the effective tax rate on a given date for the given taxId.
     * Falls back to ZERO if no rate is defined, so sales don't fail.
     */
    @Transactional(readOnly = true)
    public BigDecimal getActiveRate(UUID taxId, LocalDate date) {
        if (taxId == null) return BigDecimal.ZERO;
        return taxRateRepository.findActiveRateByTaxIdAndDate(taxId, date)
                .map(TaxRate::getRate)
                .orElse(BigDecimal.ZERO);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private Tax findTax(UUID taxId) {
        return taxRepository.findById(taxId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax", taxId));
    }

    private TaxRate saveTaxRate(Tax tax, TaxRateRequest rr) {
        TaxRate rate = TaxRate.builder()
                .tax(tax)
                .rate(rr.getRate())
                .validFrom(rr.getValidFrom())
                .validTo(rr.getValidTo())
                .build();
        return taxRateRepository.save(rate);
    }

    private TaxResponse toResponse(Tax tax) {
        List<TaxRateResponse> rates = taxRateRepository
                .findByTaxIdOrderByValidFromDesc(tax.getId()).stream()
                .map(this::toRateResponse).toList();
        return TaxResponse.builder()
                .id(tax.getId())
                .shopId(tax.getShop().getId())
                .shopName(tax.getShop().getName())
                .name(tax.getName())
                .type(tax.getType())
                .description(tax.getDescription())
                .isActive(tax.getIsActive())
                .rates(rates)
                .createdAt(tax.getCreatedAt())
                .updatedAt(tax.getUpdatedAt())
                .build();
    }

    private TaxRateResponse toRateResponse(TaxRate tr) {
        return TaxRateResponse.builder()
                .id(tr.getId())
                .taxId(tr.getTax().getId())
                .rate(tr.getRate())
                .validFrom(tr.getValidFrom())
                .validTo(tr.getValidTo())
                .createdAt(tr.getCreatedAt())
                .build();
    }
}
