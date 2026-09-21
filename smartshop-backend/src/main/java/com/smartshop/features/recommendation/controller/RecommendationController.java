package com.smartshop.features.recommendation.controller;

import com.smartshop.features.recommendation.dto.ProductRecommendationResponse;
import com.smartshop.features.recommendation.service.RecommendationService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<ProductRecommendationResponse>>> recommendations(
            @RequestParam String type,
            @RequestParam UUID shopId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.recommend(shopId, type, days, branchId)));
    }
}