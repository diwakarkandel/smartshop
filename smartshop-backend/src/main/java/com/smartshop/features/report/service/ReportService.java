package com.smartshop.features.report.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.expense.repository.ExpenseRepository;
import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.payment.repository.PaymentRepository;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.report.dto.DashboardResponse;
import com.smartshop.features.report.dto.SalesSummaryResponse;
import com.smartshop.features.report.dto.TopProductResponse;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.sale.repository.SaleRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
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
        BigDecimal profit = sales.subtract(cogs).setScale(2, RoundingMode.HALF_UP);

        long lowStockCount = 0;
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
            if (!branch.getShop().getId().equals(shopId)) {
                throw new BadRequestException("Branch does not belong to the given shop");
            }
            lowStockCount = inventoryRepository.countLowStockByBranch(branchId);
        }

        List<TopProductResponse> topProducts = saleItemRepository.topProductsByRange(shopId, day, day).stream()
                .limit(10)
                .map(row -> TopProductResponse.builder()
                        .productId((UUID) row[0])
                        .productName((String) row[1])
                        .sku((String) row[2])
                        .quantitySold((BigDecimal) row[3])
                        .revenue((BigDecimal) row[4])
                        .build())
                .toList();

        Map<String, BigDecimal> paymentBreakdown = paymentRepository.sumByShopAndMethod(
                        shopId, day.atStartOfDay(), day.atTime(LocalTime.MAX))
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Enum<?>) row[0]).name(),
                        row -> (BigDecimal) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new));

        log.info("Dashboard generated: totalSales={}, profit={}", sales, profit);
        return DashboardResponse.builder()
                .date(day)
                .totalSales(sales)
                .salesCount(saleCount)
                .totalPurchases(purchases)
                .totalExpenses(expenses)
                .grossProfit(profit)
                .lowStockCount(lowStockCount)
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
        BigDecimal profit = sales.subtract(cogs).setScale(2, RoundingMode.HALF_UP);
        log.info("Sales summary generated: totalSales={}, grossProfit={}", sales, profit);
        return SalesSummaryResponse.builder()
                .dateFrom(start)
                .dateTo(end)
                .totalSales(sales)
                .totalVat(vat)
                .totalDiscount(discount)
                .totalCogs(cogs)
                .grossProfit(profit)
                .saleCount(saleRepository.countByShopAndRange(shopId, start, end))
                .build();
    }
}