package com.smartshop.features.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryResponse {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal totalSales;
    private BigDecimal totalVat;
    private BigDecimal totalDiscount;
    private BigDecimal totalCogs;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    /** Net profit = gross profit (revenue - COGS) minus total operating expenses in the period. */
    private BigDecimal netProfit;
    private long saleCount;
}