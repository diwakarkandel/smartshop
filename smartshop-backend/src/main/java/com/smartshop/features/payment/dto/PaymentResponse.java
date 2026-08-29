package com.smartshop.features.payment.dto;

import com.smartshop.shared.enumeration.PaymentMethod;
import com.smartshop.shared.enumeration.PaymentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID saleId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentState paymentStatus;
    private String referenceNumber;
    private LocalDateTime paidAt;
    private UUID receivedById;
    private String receivedByName;
}