package com.smartshop.features.purchaseReturn.dto;

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
public class PurchaseReturnItemResponse {

    private UUID id;
    private UUID purchaseItemId;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
}