package com.smartshop.features.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private LocalDate date;
    private BigDecimal totalSales;
    private long salesCount;
    private BigDecimal totalPurchases;
    private BigDecimal totalExpenses;
    private BigDecimal grossProfit;
    /** Net profit = gross profit (revenue - COGS) minus total operating expenses. */
    private BigDecimal netProfit;
    private long lowStockCount;
    /** Products that have stock on hand but zero sales in the last 90 days. */
    private long slowMovingCount;
    private List<TopProductResponse> topProducts;
    private Map<String, BigDecimal> paymentBreakdown;
}