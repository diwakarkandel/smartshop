package com.smartshop.features.purchase.dto;

import com.smartshop.shared.enumeration.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private UUID id;
    private String purchaseNumber;
    private LocalDate purchaseDate;
    private UUID shopId;
    private UUID branchId;
    private String branchName;
    private String branchCode;
    private UUID supplierId;
    private String supplierName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private String notes;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PurchaseItemResponse> items;
}