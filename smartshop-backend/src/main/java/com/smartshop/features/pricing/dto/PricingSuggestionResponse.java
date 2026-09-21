package com.smartshop.features.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingSuggestionResponse {

    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal effectiveCost;
    private BigDecimal targetMargin;
    private BigDecimal suggestedSellingPrice;
    private BigDecimal currentSellingPrice;
    private BigDecimal currentMarginPercent;
}