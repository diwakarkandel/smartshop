package com.smartshop.features.report.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.expense.repository.ExpenseRepository;
import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.payment.repository.PaymentRepository;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.report.dto.CategoryProfitResponse;
import com.smartshop.features.report.dto.DashboardResponse;
import com.smartshop.features.report.dto.ExpenseSummaryResponse;
import com.smartshop.features.report.dto.InventoryValuationResponse;
import com.smartshop.features.report.dto.SalesSummaryResponse;
import com.smartshop.features.report.dto.TopProductResponse;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.sale.repository.SaleRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import static com.smartshop.shared.util.NumberUtils.toBigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int SLOW_MOVING_LOOKBACK_DAYS = 90;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID shopId, UUID branchId, LocalDate date) {
        log.info("Generating dashboard report for shop: {}, branch: {}, date: {}", shopId, branchId, date);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        LocalDate day = date == null ? LocalDate.now() : date;
        BigDecimal sales = saleRepository.sumTotalByShopAndDate(shopId, day);
        long saleCount = saleRepository.countByShopAndDate(shopId, day);
        BigDecimal purchases = purchaseRepository.sumTotalByShopAndDate(shopId, day);
        BigDecimal expenses = expenseRepository.sumByShopAndDate(shopId, day);
        BigDecimal cogs = saleItemRepository.sumCogsByShopAndRange(shopId, day, day);
        BigDecimal grossProfit = sales.subtract(cogs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netProfit = grossProfit.subtract(expenses).setScale(2, RoundingMode.HALF_UP);

        long lowStockCount = 0;
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
            if (!branch.getShop().getId().equals(shopId)) {
                throw new BadRequestException("Branch does not belong to the given shop");
            }
            lowStockCount = inventoryRepository.countLowStockByBranch(branchId);
        }

        LocalDate slowMovingSince = day.minusDays(SLOW_MOVING_LOOKBACK_DAYS);
        long slowMovingCount = inventoryRepository.countSlowMovingByShop(shopId, slowMovingSince);

        List<TopProductResponse> topProducts = saleItemRepository.productPerformanceByRange(shopId, day, day).stream()
                .sorted((a, b) -> toBigDecimal(b[3]).compareTo(toBigDecimal(a[3])))
                .limit(10)
                .map(row -> TopProductResponse.builder()
                        .productId((UUID) row[0])
                        .productName((String) row[1])
                        .sku((String) row[2])
                        .quantitySold(toBigDecimal(row[3]))
                        .revenue(toBigDecimal(row[4]))
                        .build())
                .toList();

        Map<String, BigDecimal> paymentBreakdown = paymentRepository.sumByShopAndMethod(
                        shopId, day.atStartOfDay(), day.atTime(LocalTime.MAX))
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Enum<?>) row[0]).name(),
                        row -> toBigDecimal(row[1]),
                        (a, b) -> a,
                        LinkedHashMap::new));

        log.info("Dashboard generated: totalSales={}, grossProfit={}, netProfit={}, slowMoving={}",
                sales, grossProfit, netProfit, slowMovingCount);
        return DashboardResponse.builder()
                .date(day)
                .totalSales(sales)
                .salesCount(saleCount)
                .totalPurchases(purchases)
                .totalExpenses(expenses)
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .lowStockCount(lowStockCount)
                .slowMovingCount(slowMovingCount)
                .topProducts(topProducts)
                .paymentBreakdown(paymentBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(UUID shopId, LocalDate from, LocalDate to) {
        log.info("Generating sales summary for shop: {}, from: {}, to: {}", shopId, from, to);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        BigDecimal sales = saleRepository.sumTotalByShopAndRange(shopId, start, end);
        BigDecimal vat = saleRepository.sumVatByShopAndRange(shopId, start, end);
        BigDecimal discount = saleRepository.sumDiscountByShopAndRange(shopId, start, end);
        BigDecimal cogs = saleItemRepository.sumCogsByShopAndRange(shopId, start, end);
        BigDecimal grossProfit = sales.subtract(cogs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpenses = expenseRepository.sumByShopAndRange(shopId, start, end);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);
        log.info("Sales summary: totalSales={}, grossProfit={}, netProfit={}", sales, grossProfit, netProfit);
        return SalesSummaryResponse.builder()
                .dateFrom(start)
                .dateTo(end)
                .totalSales(sales)
                .totalVat(vat)
                .totalDiscount(discount)
                .totalCogs(cogs)
                .grossProfit(grossProfit)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .saleCount(saleRepository.countByShopAndRange(shopId, start, end))
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse expenseSummary(UUID shopId, LocalDate from, LocalDate to) {
        log.info("Generating expense summary for shop: {}, from: {}, to: {}", shopId, from, to);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        BigDecimal total = expenseRepository.sumByShopAndRange(shopId, start, end);
        Map<String, BigDecimal> byCategory = expenseRepository.sumByCategoryAndRange(shopId, start, end)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> toBigDecimal(row[1]),
                        (a, b) -> a,
                        LinkedHashMap::new));
        return ExpenseSummaryResponse.builder()
                .dateFrom(start)
                .dateTo(end)
                .totalExpenses(total)
                .byCategory(byCategory)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(UUID shopId, LocalDate from, LocalDate to, Integer limit) {
        log.info("Generating top products for shop: {}, from: {}, to: {}, limit: {}", shopId, from, to, limit);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (start.isAfter(end)) {
            throw new BadRequestException("dateFrom must not be after dateTo");
        }
        int max = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        return saleItemRepository.productPerformanceByRange(shopId, start, end).stream()
                .sorted((a, b) -> toBigDecimal(b[3]).compareTo(toBigDecimal(a[3])))
                .limit(max)
                .map(row -> TopProductResponse.builder()
                        .productId((UUID) row[0])
                        .productName((String) row[1])
                        .sku((String) row[2])
                        .quantitySold(toBigDecimal(row[3]))
                        .revenue(toBigDecimal(row[4]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryValuationResponse> inventoryValuation(UUID shopId, UUID branchId) {
        log.info("Generating inventory valuation for shop: {}, branch: {}", shopId, branchId);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        return inventoryRepository.inventoryValuation(shopId, branchId).stream()
                .map(row -> InventoryValuationResponse.builder()
                        .branchId((UUID) row[0])
                        .branchName((String) row[1])
                        .productId((UUID) row[2])
                        .productName((String) row[3])
                        .sku((String) row[4])
                        .quantityAvailable(toBigDecimal(row[5]))
                        .averageCost(toBigDecimal(row[6]))
                        .totalValue(toBigDecimal(row[7]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryProfitResponse profitByCategory(UUID shopId, LocalDate from, LocalDate to) {
        log.info("Generating profit-by-category for shop: {}, from: {}, to: {}", shopId, from, to);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        List<CategoryProfitResponse.CategoryRow> rows = saleItemRepository
                .profitByCategoryAndRange(shopId, start, end)
                .stream()
                .map(row -> {
                    BigDecimal revenue = toBigDecimal(row[2]);
                    BigDecimal cogs = toBigDecimal(row[3]);
                    BigDecimal profit = toBigDecimal(row[4]);
                    BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) > 0
                            ? profit.divide(revenue, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return CategoryProfitResponse.CategoryRow.builder()
                            .categoryId((UUID) row[0])
                            .categoryName((String) row[1])
                            .totalRevenue(revenue)
                            .totalCogs(cogs)
                            .totalProfit(profit)
                            .profitMarginPct(margin)
                            .totalQty(toBigDecimal(row[5]))
                            .build();
                })
                .toList();
        return CategoryProfitResponse.builder()
                .dateFrom(start)
                .dateTo(end)
                .categories(rows)
                .build();
    }

}