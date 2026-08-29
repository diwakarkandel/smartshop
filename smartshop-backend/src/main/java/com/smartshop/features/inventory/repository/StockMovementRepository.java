package com.smartshop.features.inventory.repository;

import com.smartshop.features.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {
}