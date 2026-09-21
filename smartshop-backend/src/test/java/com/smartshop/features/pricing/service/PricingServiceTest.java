package com.smartshop.features.pricing.service;

import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.pricing.dto.PricingSuggestionResponse;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BranchScopeGuard branchScopeGuard;

    @InjectMocks
    private PricingService pricingService;

    private Product product(UUID id, UUID shopId, BigDecimal purchasePrice, BigDecimal sellingPrice) {
        Shop shop = new Shop();
        shop.setId(shopId);
        Product product = new Product();
        product.setId(id);
        product.setShop(shop);
        product.setName("Widget");
        product.setSku("SKU-1");
        product.setPurchasePrice(purchasePrice);
        product.setSellingPrice(sellingPrice);
        return product;
    }

    @Test
    void effectiveCostUsesWeightedAverageWhenInventoryPresent() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Product product = product(productId, shopId, new BigDecimal("50.00"), new BigDecimal("80.00"));
        // row = [productId, totalQty, totalValue]; 200 / 10 = 20.00
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.of(new Object[]{productId, new BigDecimal("10"), new BigDecimal("200")}));

        BigDecimal cost = pricingService.effectiveCost(product, shopId);

        assertThat(cost).isEqualByComparingTo("20.00");
    }

    @Test
    void effectiveCostFallsBackToPurchasePriceWhenNoInventory() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Product product = product(productId, shopId, new BigDecimal("42.00"), new BigDecimal("80.00"));
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.empty());

        BigDecimal cost = pricingService.effectiveCost(product, shopId);

        assertThat(cost).isEqualByComparingTo("42.00");
    }

    @Test
    void effectiveCostReturnsZeroWhenNoInventoryAndNoPurchasePrice() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Product product = product(productId, shopId, null, null);
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.empty());

        assertThat(pricingService.effectiveCost(product, shopId)).isEqualByComparingTo("0");
    }

    @Test
    void suggestAppliesDefaultMarginAndComputesCurrentMargin() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Product product = product(productId, shopId, new BigDecimal("50.00"), new BigDecimal("120.00"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.of(new Object[]{productId, new BigDecimal("10"), new BigDecimal("1000")}));

        PricingSuggestionResponse response;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            response = pricingService.suggest(productId, null);
        }

        verify(branchScopeGuard).requireShopAccess(userId, shopId);
        assertThat(response.getEffectiveCost()).isEqualByComparingTo("100.00");
        assertThat(response.getTargetMargin()).isEqualByComparingTo("20");
        // 100 * 1.20 = 120.00
        assertThat(response.getSuggestedSellingPrice()).isEqualByComparingTo("120.00");
        // (120 - 100) / 120 * 100 = 16.67
        assertThat(response.getCurrentMarginPercent()).isEqualByComparingTo("16.67");
    }

    @Test
    void suggestHonoursExplicitMargin() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Product product = product(productId, shopId, new BigDecimal("40.00"), BigDecimal.ZERO);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.empty());

        PricingSuggestionResponse response;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(UUID.randomUUID());
            response = pricingService.suggest(productId, new BigDecimal("50"));
        }

        // cost 40 * 1.50 = 60.00, no current margin when selling price is zero
        assertThat(response.getSuggestedSellingPrice()).isEqualByComparingTo("60.00");
        assertThat(response.getCurrentMarginPercent()).isNull();
    }

    @Test
    void suggestThrowsWhenProductMissing() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.suggest(productId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void suggestSellingPriceUsesDefaultMarginWhenNull() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Product product = product(productId, shopId, new BigDecimal("10.00"), BigDecimal.ZERO);
        when(inventoryRepository.weightedAverageCostByShopAndProduct(shopId, productId))
                .thenReturn(Optional.empty());

        // 10 * 1.20 = 12.00
        assertThat(pricingService.suggestSellingPrice(product, null)).isEqualByComparingTo("12.00");
    }
}
