package com.smartshop.features.recommendation.service;

import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.purchase.repository.PurchaseItemRepository;
import com.smartshop.features.recommendation.dto.ProductRecommendationResponse;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private PurchaseItemRepository purchaseItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BranchScopeGuard branchScopeGuard;

    @InjectMocks
    private RecommendationService recommendationService;

    private final UUID shopId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Product activeProduct(UUID id, String name, String sku, BigDecimal reorderLevel) {
        Shop shop = new Shop();
        shop.setId(shopId);
        Product product = new Product();
        product.setId(id);
        product.setShop(shop);
        product.setName(name);
        product.setSku(sku);
        product.setUnit("PCS");
        product.setReorderLevel(reorderLevel);
        product.setStatus(Status.ACTIVE);
        return product;
    }

    @Test
    void recommendRejectsUnknownType() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            assertThatThrownBy(() -> recommendationService.recommend(shopId, "MYSTERY", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MYSTERY");
        }
    }

    @Test
    void fastMovingScoresAndSortsDescending() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        // productPerformanceByRange row: [id, name, sku, qty, revenue, profit, cost]
        when(saleItemRepository.productPerformanceByRange(eq(shopId), any(), any())).thenReturn(List.of(
                new Object[]{p1, "Alpha", "A-1", new BigDecimal("100"), new BigDecimal("1000"), new BigDecimal("300"), BigDecimal.ZERO},
                new Object[]{p2, "Bravo", "B-1", new BigDecimal("60"), new BigDecimal("600"), new BigDecimal("200"), BigDecimal.ZERO}
        ));
        // sumQuantityByShop row: [productId, qty]
        when(inventoryRepository.sumQuantityByShop(shopId)).thenReturn(List.of(
                new Object[]{p1, new BigDecimal("50")},
                new Object[]{p2, new BigDecimal("10")}
        ));

        List<ProductRecommendationResponse> results;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            results = recommendationService.recommend(shopId, "FAST_MOVING", 30, null);
        }

        assertThat(results).hasSize(2);
        // p2 score = 60/10 = 6.00 outranks p1 score = 100/50 = 2.00
        assertThat(results.get(0).getProductId()).isEqualTo(p2);
        assertThat(results.get(0).getScore()).isEqualByComparingTo("6.00");
        assertThat(results.get(1).getProductId()).isEqualTo(p1);
        assertThat(results.get(1).getScore()).isEqualByComparingTo("2.00");
        // avgDaily for p1 = 100 / 30 = 3.33
        assertThat(results.get(1).getAvgDailySales()).isEqualByComparingTo("3.33");
    }

    @Test
    void slowMovingFlagsDeadStock() {
        UUID p1 = UUID.randomUUID();
        Product product = activeProduct(p1, "Dusty", "D-1", BigDecimal.ZERO);
        when(productRepository.findByShopIdAndStatusOrderByNameAsc(shopId, Status.ACTIVE))
                .thenReturn(List.of(product));
        when(inventoryRepository.sumQuantityByShop(shopId)).thenReturn(List.<Object[]>of(
                new Object[]{p1, new BigDecimal("25")}
        ));
        // no sales in range -> dead stock
        when(saleItemRepository.quantityByProductAndRange(eq(shopId), any(), any())).thenReturn(List.of());
        when(saleItemRepository.lastSaleDatePerProduct(eq(shopId), any())).thenReturn(List.of());

        List<ProductRecommendationResponse> results;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            results = recommendationService.recommend(shopId, "SLOW_MOVING", null, null);
        }

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("DEAD_STOCK");
        assertThat(results.get(0).getCurrentStock()).isEqualByComparingTo("25");
        assertThat(results.get(0).getQuantitySold()).isEqualByComparingTo("0");
    }

    @Test
    void slowMovingSkipsProductsWithoutStock() {
        UUID p1 = UUID.randomUUID();
        Product product = activeProduct(p1, "Empty", "E-1", BigDecimal.ZERO);
        when(productRepository.findByShopIdAndStatusOrderByNameAsc(shopId, Status.ACTIVE))
                .thenReturn(List.of(product));
        when(inventoryRepository.sumQuantityByShop(shopId)).thenReturn(List.of());
        when(saleItemRepository.quantityByProductAndRange(eq(shopId), any(), any())).thenReturn(List.of());
        when(saleItemRepository.lastSaleDatePerProduct(eq(shopId), any())).thenReturn(List.of());

        List<ProductRecommendationResponse> results;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            results = recommendationService.recommend(shopId, "SLOW_MOVING", null, null);
        }

        assertThat(results).isEmpty();
    }

    @Test
    void reorderSuggestsQuantityWhenBelowReorderLevel() {
        UUID p1 = UUID.randomUUID();
        Product product = activeProduct(p1, "Restock", "R-1", new BigDecimal("20"));
        when(productRepository.findByShopIdAndStatusOrderByNameAsc(shopId, Status.ACTIVE))
                .thenReturn(List.of(product));
        // current stock 5 is below reorder level 20
        when(inventoryRepository.sumQuantityByShop(shopId)).thenReturn(List.<Object[]>of(
                new Object[]{p1, new BigDecimal("5")}
        ));
        // sold 30 over default 30 days -> avgDaily 1.0
        when(saleItemRepository.quantityByProductAndRange(eq(shopId), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{p1, new BigDecimal("30")}
        ));
        when(purchaseItemRepository.supplierByProductOrderedByPurchaseDate(shopId)).thenReturn(List.of());

        List<ProductRecommendationResponse> results;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(userId);
            results = recommendationService.recommend(shopId, "REORDER", null, null);
        }

        assertThat(results).hasSize(1);
        ProductRecommendationResponse r = results.get(0);
        assertThat(r.getProductId()).isEqualTo(p1);
        assertThat(r.getAvgDailySales()).isEqualByComparingTo("1.00");
        // reorderLevel(20) + avgDaily(1)*7 - stock(5) = 22, rounded up
        assertThat(r.getSuggestedOrderQty()).isEqualByComparingTo("22");
    }
}
