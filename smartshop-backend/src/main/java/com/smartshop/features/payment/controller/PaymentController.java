package com.smartshop.features.payment.controller;

import com.smartshop.features.payment.dto.PaymentRequest;
import com.smartshop.features.payment.dto.PaymentResponse;
import com.smartshop.features.payment.service.PaymentService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listBySale(@RequestParam UUID saleId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.listBySale(saleId)));
    }

    @org.springframework.web.bind.annotation.PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> record(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", paymentService.recordPayment(request)));
    }
}