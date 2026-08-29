package com.smartshop.features.inventory.controller;

import com.smartshop.features.inventory.dto.InventoryResponse;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT','CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> list(
            @RequestParam UUID branchId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "productName,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStockOnly) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(inventoryService.listForBranch(branchId, search, lowStockOnly, pageable))));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT','CASHIER')")
    public ResponseEntity<ApiResponse<InventoryResponse>> get(@RequestParam UUID branchId,
                                                              @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByProduct(branchId, productId)));
    }
}