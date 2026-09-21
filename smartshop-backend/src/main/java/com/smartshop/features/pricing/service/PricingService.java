package com.smartshop.features.pricing.service;

import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.pricing.dto.PricingSuggestionResponse;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.ResourceNotFoundException;
import static com.smartshop.shared.util.NumberUtils.toBigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final BigDecimal DEFAULT_TARGET_MARGIN = new BigDecimal("20");

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final BranchScopeGuard branchScopeGuard;

    /** Weighted average landed cost (average_cost, which includes allocated extra_cost). */
    public BigDecimal effectiveCost(UUID productId, UUID shopId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return effectiveCost(product, shopId);
    }

    public BigDecimal effectiveCost(Product product, UUID shopId) {
        Object[] row = inventoryRepository.weightedAverageCostByShopAndProduct(shopId, product.getId())
                .orElse(null);
        if (row != null && row.length > 2 && row[1] != null && row[2] != null) {
            BigDecimal totalQty = toBigDecimal(row[1]);
            BigDecimal totalValue = toBigDecimal(row[2]);
            if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                return totalValue.divide(totalQty, 2, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO);
            }
        }
        BigDecimal purchasePrice = product.getPurchasePrice();
        return purchasePrice == null ? BigDecimal.ZERO : purchasePrice;
    }

    @Transactional(readOnly = true)
    public PricingSuggestionResponse suggest(UUID productId, BigDecimal margin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), product.getShop().getId());

        BigDecimal effectiveCost = effectiveCost(product, product.getShop().getId());
        BigDecimal targetMargin = margin == null
                ? DEFAULT_TARGET_MARGIN : margin.setScale(2, RoundingMode.HALF_UP);
        BigDecimal multiplier = new BigDecimal("100").add(targetMargin)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal suggested = effectiveCost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        BigDecimal sellingPrice = product.getSellingPrice() == null ? BigDecimal.ZERO : product.getSellingPrice();
        BigDecimal currentMargin = null;
        if (sellingPrice.compareTo(BigDecimal.ZERO) > 0) {
            currentMargin = sellingPrice.subtract(effectiveCost)
                    .multiply(new BigDecimal("100"))
                    .divide(sellingPrice, 2, RoundingMode.HALF_UP);
        }
        log.info("Pricing suggestion for product: {} -> cost={}, margin={}%, suggested={}",
                product.getSku(), effectiveCost, targetMargin, suggested);
        return PricingSuggestionResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .effectiveCost(effectiveCost)
                .targetMargin(targetMargin)
                .suggestedSellingPrice(suggested)
                .currentSellingPrice(sellingPrice)
                .currentMarginPercent(currentMargin)
                .build();
    }

    public BigDecimal suggestSellingPrice(Product product, BigDecimal targetMargin) {
        BigDecimal effectiveCost = effectiveCost(product, product.getShop().getId());
        BigDecimal margin = targetMargin == null ? DEFAULT_TARGET_MARGIN : targetMargin;
        BigDecimal multiplier = new BigDecimal("100").add(margin)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        return effectiveCost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}