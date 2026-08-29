package com.smartshop.features.adminDashboard.controller;

import com.smartshop.features.adminDashboard.dto.ShopSalesResponse;
import com.smartshop.features.adminDashboard.service.AdminDashboardService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/shop-sales")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ShopSalesResponse>>> getShopSales() {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getSalesByShop()));
    }
}
