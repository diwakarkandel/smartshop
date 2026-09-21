package com.smartshop.features.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryProfitResponse {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<CategoryRow> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRow {
        private UUID categoryId;
        private String categoryName;
        private BigDecimal totalRevenue;
        private BigDecimal totalCogs;
        private BigDecimal totalProfit;
        private BigDecimal profitMarginPct;
        private BigDecimal totalQty;
    }
}