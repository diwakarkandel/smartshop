package com.smartshop.features.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemRequest {

    @NotNull(message = "Product id is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.00", message = "VAT rate cannot be negative")
    private BigDecimal vatRate;
}