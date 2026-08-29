package com.smartshop.features.purchaseReturn.repository;

import com.smartshop.features.purchaseReturn.entity.PurchaseReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, UUID> {

    List<PurchaseReturnItem> findByPurchaseReturnId(UUID purchaseReturnId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM PurchaseReturnItem i WHERE i.purchaseItem.id = :purchaseItemId")
    BigDecimal sumReturnedQuantityByPurchaseItemId(@Param("purchaseItemId") UUID purchaseItemId);
}