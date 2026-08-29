package com.smartshop.features.inventory.repository;

import com.smartshop.features.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByBranchIdAndProductId(UUID branchId, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.branch.id = :branchId AND i.product.id = :productId")
    Optional<Inventory> findByBranchIdAndProductIdForUpdate(@Param("branchId") UUID branchId,
                                                            @Param("productId") UUID productId);

    List<Inventory> findByBranchIdOrderByProductNameAsc(UUID branchId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.branch.id = :branchId AND i.quantityAvailable <= i.product.reorderLevel")
    long countLowStockByBranch(@Param("branchId") UUID branchId);
}