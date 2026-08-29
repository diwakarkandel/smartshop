package com.smartshop.features.supplier.controller;

import com.smartshop.features.supplier.dto.SupplierRequest;
import com.smartshop.features.supplier.dto.SupplierResponse;
import com.smartshop.features.supplier.service.SupplierService;
import com.smartshop.shared.constant.AppConstants;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(supplierService.list(shopId, search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<SupplierResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<SupplierResponse>> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully", supplierService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully", supplierService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deactivated successfully", null));
    }
}