package com.smartshop.features.saleReturn.repository;

import com.smartshop.features.saleReturn.entity.SaleReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, UUID> {

    List<SaleReturnItem> findBySaleReturnId(UUID saleReturnId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM SaleReturnItem i WHERE i.saleItem.id = :saleItemId")
    BigDecimal sumReturnedQuantityBySaleItemId(@Param("saleItemId") UUID saleItemId);
}