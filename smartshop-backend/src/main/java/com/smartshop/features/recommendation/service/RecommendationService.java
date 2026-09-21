package com.smartshop.features.recommendation.service;

import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.purchase.repository.PurchaseItemRepository;
import com.smartshop.features.recommendation.dto.ProductRecommendationResponse;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.Status;
import static com.smartshop.shared.util.NumberUtils.toBigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_DAYS = 30;
    private static final int SLOW_MOVING_LOOKBACK_DAYS = 90;

    private final SaleItemRepository saleItemRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional(readOnly = true)
    public List<ProductRecommendationResponse> recommend(UUID shopId, String type, Integer days, UUID branchId) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        return switch (type) {
            case "FAST_MOVING" -> fastMoving(shopId, days, branchId);
            case "HIGH_PROFIT" -> highProfit(shopId, days, branchId);
            case "SLOW_MOVING" -> slowMoving(shopId, branchId);
            case "REORDER" -> reorder(shopId, branchId);
            default -> throw new IllegalArgumentException("Unknown recommendation type: " + type);
        };
    }

    private List<ProductRecommendationResponse> fastMoving(UUID shopId, Integer days, UUID branchId) {
        int d = effectiveDays(days);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(d - 1L);
        log.info("Fast moving recommendation for shop {} last {} days", shopId, d);
        Map<UUID, Object[]> perf = loadPerformance(shopId, from, to);
        Map<UUID, BigDecimal> stock = stockMap(shopId, branchId);
        List<ProductRecommendationResponse> results = new ArrayList<>();
        for (Map.Entry<UUID, Object[]> e : perf.entrySet()) {
            Object[] row = e.getValue();
            BigDecimal qty = toBigDecimal(row[3]);
            BigDecimal currentStock = stock.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal avgDaily = qty.divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP);
            BigDecimal score = currentStock.compareTo(BigDecimal.ZERO) > 0
                    ? qty.divide(currentStock, 2, RoundingMode.HALF_UP)
                    : qty;
            results.add(ProductRecommendationResponse.builder()
                    .productId(e.getKey())
                    .productName((String) row[1])
                    .sku((String) row[2])
                    .quantitySold(qty)
                    .revenue(toBigDecimal(row[4]))
                    .avgDailySales(avgDaily)
                    .currentStock(currentStock)
                    .score(score)
                    .build());
        }
        results.sort(Comparator.comparing(ProductRecommendationResponse::getScore).reversed());
        return results.stream().limit(10).toList();
    }

    private List<ProductRecommendationResponse> highProfit(UUID shopId, Integer days, UUID branchId) {
        int d = effectiveDays(days);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(d - 1L);
        log.info("High profit recommendation for shop {} last {} days", shopId, d);
        Map<UUID, Object[]> perf = loadPerformance(shopId, from, to);
        Map<UUID, BigDecimal> stock = stockMap(shopId, branchId);
        List<ProductRecommendationResponse> results = new ArrayList<>();
        for (Map.Entry<UUID, Object[]> e : perf.entrySet()) {
            Object[] row = e.getValue();
            BigDecimal revenue = toBigDecimal(row[4]);
            BigDecimal profit = toBigDecimal(row[5]);
            BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(new BigDecimal("100")).divide(revenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            results.add(ProductRecommendationResponse.builder()
                    .productId(e.getKey())
                    .productName((String) row[1])
                    .sku((String) row[2])
                    .quantitySold(toBigDecimal(row[3]))
                    .revenue(revenue)
                    .profit(profit)
                    .marginPercent(margin)
                    .currentStock(stock.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .build());
        }
        results.sort(Comparator.comparing(ProductRecommendationResponse::getProfit).reversed());
        return results.stream().limit(10).toList();
    }

    private List<ProductRecommendationResponse> slowMoving(UUID shopId, UUID branchId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(SLOW_MOVING_LOOKBACK_DAYS - 1L);
        log.info("Slow moving recommendation for shop {} last {} days", shopId, SLOW_MOVING_LOOKBACK_DAYS);
        Map<UUID, BigDecimal> stock = stockMap(shopId, branchId);
        Map<UUID, BigDecimal> sold = quantityByRange(shopId, from, to);
        Map<UUID, LocalDate> lastSale = lastSaleDateMap(shopId, from);
        List<ProductRecommendationResponse> results = new ArrayList<>();
        for (Product product : productRepository.findByShopIdAndStatusOrderByNameAsc(shopId, Status.ACTIVE)) {
            BigDecimal currentStock = stock.getOrDefault(product.getId(), BigDecimal.ZERO);
            if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal qtySold = sold.getOrDefault(product.getId(), BigDecimal.ZERO);
            boolean deadStock = qtySold.compareTo(BigDecimal.ZERO) == 0;
            boolean slowMoving = !deadStock && qtySold.compareTo(currentStock.multiply(new BigDecimal("0.10"))) < 0;
            if (!deadStock && !slowMoving) {
                continue;
            }
            LocalDate lastSaleDate = lastSale.get(product.getId());
            results.add(ProductRecommendationResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .unit(product.getUnit())
                    .quantitySold(qtySold)
                    .currentStock(currentStock)
                    .status(deadStock ? "DEAD_STOCK" : "SLOW_MOVING")
                    .lastSaleDate(lastSaleDate)
                    .suggestedAction(deadStock
                            ? "No sale in 90 days — consider discount or return to supplier"
                            : "Low sales velocity — consider promotion")
                    .build());
        }
        results.sort(Comparator.comparing(ProductRecommendationResponse::getQuantitySold));
        return results;
    }

    private List<ProductRecommendationResponse> reorder(UUID shopId, UUID branchId) {
        int d = effectiveDays(null);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(d - 1L);
        log.info("Reorder recommendation for shop {} based on last {} days velocity", shopId, d);
        Map<UUID, BigDecimal> stock = stockMap(shopId, branchId);
        Map<UUID, BigDecimal> sold = quantityByRange(shopId, from, to);
        Map<UUID, Object[]> suppliers = lastSupplierMap(shopId);
        List<ProductRecommendationResponse> results = new ArrayList<>();
        for (Product product : productRepository.findByShopIdAndStatusOrderByNameAsc(shopId, Status.ACTIVE)) {
            BigDecimal currentStock = stock.getOrDefault(product.getId(), BigDecimal.ZERO);
            BigDecimal reorderLevel = product.getReorderLevel() == null ? BigDecimal.ZERO : product.getReorderLevel();
            BigDecimal qtySold = sold.getOrDefault(product.getId(), BigDecimal.ZERO);
            if (reorderLevel.compareTo(BigDecimal.ZERO) <= 0 || currentStock.compareTo(reorderLevel) > 0) {
                continue;
            }
            BigDecimal avgDaily = qtySold.divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP);
            if (avgDaily.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal urgency = reorderLevel.subtract(currentStock).divide(avgDaily, 2, RoundingMode.HALF_UP);
            BigDecimal suggested = reorderLevel.add(avgDaily.multiply(new BigDecimal("7")))
                    .subtract(currentStock).max(BigDecimal.ZERO).setScale(0, RoundingMode.UP);
            Object[] supplier = suppliers.get(product.getId());
            results.add(ProductRecommendationResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .unit(product.getUnit())
                    .quantitySold(qtySold)
                    .currentStock(currentStock)
                    .reorderLevel(reorderLevel)
                    .avgDailySales(avgDaily)
                    .suggestedOrderQty(suggested)
                    .urgency(urgency)
                    .supplierId(supplier == null ? null : (UUID) supplier[0])
                    .supplierName(supplier == null ? null : (String) supplier[1])
                    .build());
        }
        results.sort(Comparator.comparing(ProductRecommendationResponse::getUrgency).reversed());
        return results;
    }

    private Map<UUID, Object[]> loadPerformance(UUID shopId, LocalDate from, LocalDate to) {
        Map<UUID, Object[]> map = new LinkedHashMap<>();
        for (Object[] row : saleItemRepository.productPerformanceByRange(shopId, from, to)) {
            if (row == null || row.length < 6 || row[0] == null) {
                continue;
            }
            map.put((UUID) row[0], row);
        }
        return map;
    }

    private Map<UUID, BigDecimal> quantityByRange(UUID shopId, LocalDate from, LocalDate to) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : saleItemRepository.quantityByProductAndRange(shopId, from, to)) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            map.put((UUID) row[0], toBigDecimal(row[1]));
        }
        return map;
    }

    private Map<UUID, LocalDate> lastSaleDateMap(UUID shopId, LocalDate from) {
        Map<UUID, LocalDate> map = new HashMap<>();
        for (Object[] row : saleItemRepository.lastSaleDatePerProduct(shopId, from)) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            map.put((UUID) row[0], (LocalDate) row[1]);
        }
        return map;
    }

    private Map<UUID, BigDecimal> stockMap(UUID shopId, UUID branchId) {
        List<Object[]> rows = branchId != null
                ? inventoryRepository.sumQuantityByBranch(branchId)
                : inventoryRepository.sumQuantityByShop(shopId);
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                map.put((UUID) row[0], toBigDecimal(row[1]));
            }
        }
        return map;
    }

    private Map<UUID, Object[]> lastSupplierMap(UUID shopId) {
        Map<UUID, Object[]> map = new LinkedHashMap<>();
        for (Object[] row : purchaseItemRepository.supplierByProductOrderedByPurchaseDate(shopId)) {
            UUID productId = (UUID) row[0];
            if (!map.containsKey(productId)) {
                map.put(productId, new Object[]{row[1], row[2]});
            }
        }
        return map;
    }

    private int effectiveDays(Integer days) {
        return days == null || days <= 0 ? DEFAULT_DAYS : days;
    }
}