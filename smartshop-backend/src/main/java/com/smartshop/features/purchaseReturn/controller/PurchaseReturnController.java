package com.smartshop.features.purchaseReturn.controller;

import com.smartshop.features.purchaseReturn.dto.PurchaseReturnRequest;
import com.smartshop.features.purchaseReturn.dto.PurchaseReturnResponse;
import com.smartshop.features.purchaseReturn.service.PurchaseReturnService;
import com.smartshop.shared.constant.AppConstants;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-returns")
@RequiredArgsConstructor
@Tag(name = "Purchase Returns")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseReturnResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "returnDate"));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                purchaseReturnService.list(shopId, branchId, dateFrom, dateTo, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> create(@Valid @RequestBody PurchaseReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase return recorded successfully", purchaseReturnService.create(request)));
    }
}