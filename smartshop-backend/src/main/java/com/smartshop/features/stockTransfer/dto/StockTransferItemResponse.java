package com.smartshop.features.stockTransfer.dto;

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
public class StockTransferItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal quantity;
}