package com.smartshop.features.report.controller;

import com.smartshop.features.report.dto.CategoryProfitResponse;
import com.smartshop.features.report.dto.DashboardResponse;
import com.smartshop.features.report.dto.ExpenseSummaryResponse;
import com.smartshop.features.report.dto.InventoryValuationResponse;
import com.smartshop.features.report.dto.SalesSummaryResponse;
import com.smartshop.features.report.dto.TopProductResponse;
import com.smartshop.features.report.service.ReportService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
            @RequestParam UUID shopId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(reportService.dashboard(shopId, branchId, date)));
    }

    @GetMapping("/sales-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<SalesSummaryResponse>> salesSummary(
            @RequestParam UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(reportService.salesSummary(shopId, dateFrom, dateTo)));
    }

    @GetMapping("/expense-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> expenseSummary(
            @RequestParam UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(reportService.expenseSummary(shopId, dateFrom, dateTo)));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> topProducts(
            @RequestParam UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.topProducts(shopId, dateFrom, dateTo, limit)));
    }

    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<InventoryValuationResponse>>> inventoryValuation(
            @RequestParam UUID shopId,
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.inventoryValuation(shopId, branchId)));
    }

    @GetMapping("/profit-by-category")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<CategoryProfitResponse>> profitByCategory(
            @RequestParam UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(reportService.profitByCategory(shopId, dateFrom, dateTo)));
    }
}
