package com.smartshop.features.product.controller;

import com.smartshop.features.product.dto.ProductRequest;
import com.smartshop.features.product.dto.ProductResponse;
import com.smartshop.features.product.dto.ProductSearchResponse;
import com.smartshop.features.product.service.ProductService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.Status;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<ProductSearchResponse>>> search(@RequestParam UUID branchId,
                                                                           @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(productService.search(branchId, query)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Status status) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(productService.list(shopId, search, categoryId, status, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully", null));
    }
}