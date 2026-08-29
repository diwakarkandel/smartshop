package com.smartshop.features.stockTransfer.controller;

import com.smartshop.features.stockTransfer.dto.StockTransferRequest;
import com.smartshop.features.stockTransfer.dto.StockTransferResponse;
import com.smartshop.features.stockTransfer.service.StockTransferService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.TransferStatus;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Stock Transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<StockTransferResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) UUID branchId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                stockTransferService.list(shopId, status, branchId, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(stockTransferService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> create(@Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock transfer created successfully", stockTransferService.create(request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Stock transfer approved successfully", stockTransferService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Stock transfer rejected successfully", stockTransferService.reject(id)));
    }
}