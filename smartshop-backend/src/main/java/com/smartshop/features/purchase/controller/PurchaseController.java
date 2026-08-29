package com.smartshop.features.purchase.controller;

import com.smartshop.features.purchase.dto.PurchaseRequest;
import com.smartshop.features.purchase.dto.PurchaseResponse;
import com.smartshop.features.purchase.service.PurchaseService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.dto.PaymentStatusRequest;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@Tag(name = "Purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "purchaseDate,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                purchaseService.list(shopId, search, branchId, paymentStatus, dateFrom, dateTo, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase created successfully", purchaseService.create(request)));
    }

    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updatePaymentStatus(@PathVariable UUID id,
                                                                              @Valid @RequestBody PaymentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully",
                purchaseService.updatePaymentStatus(id, request)));
    }
}