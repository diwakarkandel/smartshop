package com.smartshop.features.sale.dto;

import com.smartshop.shared.enumeration.PaymentMethod;
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
public class SaleRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotNull(message = "Branch id is required")
    private UUID branchId;

    private UUID customerId;

    private LocalDate billDate;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "QR reference must be at most 100 characters")
    private String qrReference;

    @DecimalMin(value = "0.00", message = "Cash tendered cannot be negative")
    private BigDecimal cashTendered;

    @Size(max = 1000, message = "Remarks must be at most 1000 characters")
    private String remarks;

    @NotEmpty(message = "At least one sale item is required")
    @Valid
    private List<SaleItemRequest> items;
}