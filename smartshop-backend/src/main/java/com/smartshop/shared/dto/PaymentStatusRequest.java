package com.smartshop.shared.dto;

import com.smartshop.shared.enumeration.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusRequest {

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}