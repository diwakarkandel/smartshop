package com.smartshop.features.sale.controller;

import com.smartshop.features.sale.dto.SaleRequest;
import com.smartshop.features.sale.dto.SaleResponse;
import com.smartshop.features.sale.service.SaleService;
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
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales / POS")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<SaleResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "billDate,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                saleService.list(shopId, search, branchId, paymentStatus, dateFrom, dateTo, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<SaleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(saleService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<SaleResponse>> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale completed successfully", saleService.create(request)));
    }

    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<SaleResponse>> updatePaymentStatus(@PathVariable UUID id,
                                                                          @Valid @RequestBody PaymentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully",
                saleService.updatePaymentStatus(id, request)));
    }
}