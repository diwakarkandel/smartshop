package com.smartshop.features.purchase.dto;

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
public class PurchaseItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal discountAmount;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal lineTotal;
}