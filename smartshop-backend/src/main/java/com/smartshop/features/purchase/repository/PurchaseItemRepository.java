package com.smartshop.features.purchase.repository;

import com.smartshop.features.purchase.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    boolean existsByProductId(UUID productId);

    List<PurchaseItem> findByPurchaseId(UUID purchaseId);

    @Query("SELECT pi.product.id, p.supplier.id, p.supplier.name, p.purchaseDate "
            + "FROM PurchaseItem pi JOIN pi.purchase p WHERE p.shop.id = :shopId ORDER BY p.purchaseDate DESC")
    List<Object[]> supplierByProductOrderedByPurchaseDate(@Param("shopId") UUID shopId);
}