package com.smartshop.features.adminDashboard.service;

import com.smartshop.features.adminDashboard.dto.ShopSalesProjection;
import com.smartshop.features.adminDashboard.dto.ShopSalesResponse;
import com.smartshop.features.sale.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public List<ShopSalesResponse> getSalesByShop() {
        log.info("Aggregating sales by shop for admin dashboard");
        List<ShopSalesResponse> result = saleRepository.aggregateSalesByShop().stream()
                .map(this::toResponse)
                .toList();
        log.info("Aggregated sales data for {} shops", result.size());
        return result;
    }

    private ShopSalesResponse toResponse(ShopSalesProjection projection) {
        return ShopSalesResponse.builder()
                .shopId(projection.getShopId())
                .shopName(projection.getShopName())
                .totalSales(projection.getTotalSales())
                .build();
    }
}
