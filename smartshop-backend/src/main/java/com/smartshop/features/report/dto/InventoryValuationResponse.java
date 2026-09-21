package com.smartshop.features.report.dto;

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
public class InventoryValuationResponse {

    private UUID branchId;
    private String branchName;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal quantityAvailable;
    private BigDecimal averageCost;
    /** totalValue = quantityAvailable * averageCost */
    private BigDecimal totalValue;
}
