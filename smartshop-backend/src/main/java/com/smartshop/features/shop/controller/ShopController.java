package com.smartshop.features.shop.controller;

import com.smartshop.features.shop.dto.ShopRequest;
import com.smartshop.features.shop.dto.ShopResponse;
import com.smartshop.features.shop.service.ShopService;
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
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shops")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<ShopResponse>>> listShops(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(shopService.listShops(search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ShopResponse>> getShop(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(shopService.getShop(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShopResponse>> createShop(@Valid @RequestBody ShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shop created successfully", shopService.createShop(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(@PathVariable UUID id,
                                                                @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shop updated successfully", shopService.updateShop(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteShop(@PathVariable UUID id) {
        shopService.deleteShop(id);
        return ResponseEntity.ok(ApiResponse.success("Shop deactivated successfully", null));
    }
}