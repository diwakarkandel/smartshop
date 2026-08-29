package com.smartshop.features.adminDashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface ShopSalesProjection {

    UUID getShopId();

    String getShopName();

    BigDecimal getTotalSales();
}
