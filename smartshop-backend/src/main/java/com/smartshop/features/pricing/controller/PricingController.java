package com.smartshop.features.pricing.controller;

import com.smartshop.features.pricing.dto.PricingSuggestionResponse;
import com.smartshop.features.pricing.service.PricingService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing")
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT','INVENTORY_STAFF')")
    public ResponseEntity<ApiResponse<PricingSuggestionResponse>> suggest(
            @RequestParam UUID productId,
            @RequestParam(required = false) BigDecimal margin) {
        return ResponseEntity.ok(ApiResponse.success(pricingService.suggest(productId, margin)));
    }
}