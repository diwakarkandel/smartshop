package com.smartshop.features.stockTransfer.repository;

import com.smartshop.features.stockTransfer.entity.StockTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, UUID> {

    List<StockTransferItem> findByStockTransferId(UUID stockTransferId);
}