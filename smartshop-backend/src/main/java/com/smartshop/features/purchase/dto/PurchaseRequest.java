package com.smartshop.features.purchase.dto;

import com.smartshop.shared.enumeration.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotNull(message = "Branch id is required")
    private UUID branchId;

    @NotNull(message = "Supplier id is required")
    private UUID supplierId;

    private LocalDate purchaseDate;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    private PaymentStatus paymentStatus;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one purchase item is required")
    @Valid
    private List<PurchaseItemRequest> items;
}