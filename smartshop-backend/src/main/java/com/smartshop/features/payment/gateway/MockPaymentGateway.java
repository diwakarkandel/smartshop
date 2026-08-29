package com.smartshop.features.payment.gateway;

import com.smartshop.shared.enumeration.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult process(PaymentMethod method, BigDecimal amount, String referenceNumber) {
        if (method == PaymentMethod.CASH) {
            return PaymentResult.builder()
                    .success(true)
                    .referenceNumber(referenceNumber)
                    .message("Cash payment processed")
                    .build();
        }
        return PaymentResult.builder()
                .success(true)
                .referenceNumber(referenceNumber != null ? referenceNumber : "MOCK-" + System.nanoTime())
                .message("Mock " + method.name() + " payment processed (no real gateway integration yet)")
                .build();
    }
}