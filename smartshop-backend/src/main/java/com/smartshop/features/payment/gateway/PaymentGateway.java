package com.smartshop.features.payment.gateway;

import com.smartshop.shared.enumeration.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult process(PaymentMethod method, BigDecimal amount, String referenceNumber);
}