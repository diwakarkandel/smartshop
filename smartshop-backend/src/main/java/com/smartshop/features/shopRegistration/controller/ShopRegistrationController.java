package com.smartshop.features.shopRegistration.controller;

import com.smartshop.features.shopRegistration.dto.ShopRegistrationApplyRequest;
import com.smartshop.features.shopRegistration.dto.ShopRegistrationRejectRequest;
import com.smartshop.features.shopRegistration.dto.ShopRegistrationResponse;
import com.smartshop.features.shopRegistration.service.ShopRegistrationService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.RegistrationStatus;
import com.smartshop.shared.exception.BadRequestException;
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
@RequestMapping("/api/v1/shop-registrations")
@RequiredArgsConstructor
@Tag(name = "Shop Registrations")
public class ShopRegistrationController {

    private final ShopRegistrationService shopRegistrationService;

    /**
     * Only users who do NOT already have a SUPER_ADMIN or SHOP_ADMIN role may apply.
     * - SUPER_ADMIN manages the platform; they approve/reject applications, not submit them.
     * - An existing SHOP_ADMIN already owns a shop; they cannot register a second shop via this flow.
     */
    @PostMapping("/apply")
    @PreAuthorize("!hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<ShopRegistrationResponse>> apply(
            @Valid @RequestBody ShopRegistrationApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shop registration application submitted",
                        shopRegistrationService.apply(request)));
    }

    /** Applicant views their own submitted application — blocked for SUPER_ADMIN (they use GET /) */
    @GetMapping("/my-application")
    @PreAuthorize("!hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShopRegistrationResponse>> getMyApplication() {
        return ResponseEntity.ok(ApiResponse.success(shopRegistrationService.getMyApplication()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ShopRegistrationResponse>>> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT) String sort) {
        RegistrationStatus statusFilter = parseStatus(status);
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(shopRegistrationService.listApplications(statusFilter, pageable))));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShopRegistrationResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Application approved successfully",
                shopRegistrationService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ShopRegistrationResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ShopRegistrationRejectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application rejected",
                shopRegistrationService.reject(id, request)));
    }

    private RegistrationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return RegistrationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status filter. Allowed values: PENDING, APPROVED, REJECTED");
        }
    }
}
