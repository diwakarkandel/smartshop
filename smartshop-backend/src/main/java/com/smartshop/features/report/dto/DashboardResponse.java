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
    private long lowStockCount;
    private List<TopProductResponse> topProducts;
    private Map<String, BigDecimal> paymentBreakdown;
}