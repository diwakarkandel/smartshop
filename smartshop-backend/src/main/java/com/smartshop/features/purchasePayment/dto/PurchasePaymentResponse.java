package com.smartshop.features.purchasePayment.dto;

import com.smartshop.shared.enumeration.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PurchasePaymentResponse {
    private UUID id;
    private UUID purchaseId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String notes;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
