package com.smartshop.features.purchasePayment.controller;

import com.smartshop.features.purchasePayment.dto.PurchasePaymentRequest;
import com.smartshop.features.purchasePayment.dto.PurchasePaymentResponse;
import com.smartshop.features.purchasePayment.service.PurchasePaymentService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchases/{purchaseId}/payments")
@RequiredArgsConstructor
@Tag(name = "Purchase Payments", description = "Track payments made to suppliers for purchase orders")
public class PurchasePaymentController {

    private final PurchasePaymentService purchasePaymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER')")
    @Operation(summary = "Record a new payment for a purchase")
    public ResponseEntity<ApiResponse<PurchasePaymentResponse>> recordPayment(
            @PathVariable UUID purchaseId,
            @Valid @RequestBody PurchasePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Payment recorded successfully",
                        purchasePaymentService.recordPayment(purchaseId, request)
                ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER')")
    @Operation(summary = "List all payments for a purchase")
    public ResponseEntity<ApiResponse<List<PurchasePaymentResponse>>> listPayments(
            @PathVariable UUID purchaseId) {
        return ResponseEntity.ok(ApiResponse.success(
                purchasePaymentService.listPayments(purchaseId)
        ));
    }
}
