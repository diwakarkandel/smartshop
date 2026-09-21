package com.smartshop.features.product.dto;

import com.smartshop.shared.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String sku;
    private String barcode;
    private String brand;
    private String unit;
    private String description;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal effectiveCost;
    private BigDecimal suggestedSellingPrice;
    private BigDecimal profitMarginPercent;
    private Boolean vatApplicable;
    private BigDecimal vatRate;
    private UUID taxId;
    private String taxName;
    private BigDecimal reorderLevel;
    private String imageUrl;
    private Status status;
    private List<String> warnings;
    private LocalDateTime createdAt;
}