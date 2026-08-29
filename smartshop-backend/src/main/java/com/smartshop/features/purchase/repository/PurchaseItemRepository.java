package com.smartshop.features.purchase.repository;

import com.smartshop.features.purchase.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    boolean existsByProductId(UUID productId);

    List<PurchaseItem> findByPurchaseId(UUID purchaseId);
}