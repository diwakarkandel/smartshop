package com.smartshop.features.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendationResponse {

    private UUID productId;
    private String productName;
    private String sku;
    private String unit;
    private BigDecimal quantitySold;
    private BigDecimal revenue;
    private BigDecimal profit;
    private BigDecimal marginPercent;
    private BigDecimal currentStock;
    private BigDecimal reorderLevel;
    private BigDecimal suggestedOrderQty;
    private BigDecimal avgDailySales;
    private BigDecimal score;
    private BigDecimal urgency;
    private String status;
    private LocalDate lastSaleDate;
    private String suggestedAction;
    private UUID supplierId;
    private String supplierName;
}