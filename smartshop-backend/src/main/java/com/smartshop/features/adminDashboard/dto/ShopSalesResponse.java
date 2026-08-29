package com.smartshop.features.adminDashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSalesResponse {

    private UUID shopId;
    private String shopName;
    private BigDecimal totalSales;
}
