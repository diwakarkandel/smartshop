package com.smartshop.features.product.dto;

import com.smartshop.shared.enumeration.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    private UUID categoryId;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be at most 200 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be at most 50 characters")
    private String sku;

    @Size(max = 100, message = "Barcode must be at most 100 characters")
    private String barcode;

    @Size(max = 100, message = "Brand must be at most 100 characters")
    private String brand;

    @Size(max = 20, message = "Unit must be at most 20 characters")
    private String unit;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    private Boolean vatApplicable;

    private BigDecimal vatRate;

    /** Optional: reference to a configured Tax entity. When set, vatRate on the product is ignored during checkout. */
    private UUID taxId;

    @DecimalMin(value = "0.00", message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;

    private Status status;
}