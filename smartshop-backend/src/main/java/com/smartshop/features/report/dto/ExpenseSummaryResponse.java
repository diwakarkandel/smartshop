package com.smartshop.features.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryResponse {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal totalExpenses;
    /** Category name to total amount mapping, ordered by amount descending. */
    private Map<String, BigDecimal> byCategory;
}
