package com.smartshop.features.product.dto;

import com.smartshop.shared.enumeration.Status;
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
public class ProductSearchResponse {

    private UUID id;
    private String name;
    private String sku;
    private String barcode;
    private String brand;
    private String unit;
    private BigDecimal sellingPrice;
    private BigDecimal vatRate;
    private Boolean vatApplicable;
    private Status status;
    private BigDecimal quantityAvailable;
    private String stockStatus;
}