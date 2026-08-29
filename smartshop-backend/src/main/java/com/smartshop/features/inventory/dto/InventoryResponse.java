package com.smartshop.features.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID id;
    private UUID branchId;
    private UUID productId;
    private String productName;
    private String sku;
    private String barcode;
    private String categoryName;
    private String unit;
    private BigDecimal quantityAvailable;
    private BigDecimal quantityReserved;
    private BigDecimal averageCost;
    private BigDecimal reorderLevel;
    private String stockStatus;
    private LocalDateTime lastStockUpdate;
}