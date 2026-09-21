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

    @Query("SELECT i.product.id, COALESCE(SUM(i.quantityAvailable), 0), COALESCE(SUM(i.quantityAvailable * i.averageCost), 0) "
            + "FROM Inventory i WHERE i.branch.shop.id = :shopId AND i.product.id = :productId GROUP BY i.product.id")
    Optional<Object[]> weightedAverageCostByShopAndProduct(@Param("shopId") UUID shopId,
                                                           @Param("productId") UUID productId);

    @Query("SELECT i.product.id, COALESCE(SUM(i.quantityAvailable), 0) "
            + "FROM Inventory i WHERE i.branch.shop.id = :shopId GROUP BY i.product.id")
    List<Object[]> sumQuantityByShop(@Param("shopId") UUID shopId);

    @Query("SELECT i.product.id, COALESCE(SUM(i.quantityAvailable), 0) "
            + "FROM Inventory i WHERE i.branch.id = :branchId GROUP BY i.product.id")
    List<Object[]> sumQuantityByBranch(@Param("branchId") UUID branchId);

    /** Inventory valuation for a shop, optionally filtered by branch. */
    @Query("SELECT i.branch.id, i.branch.name, i.product.id, i.product.name, i.product.sku, "
            + "i.quantityAvailable, i.averageCost, (i.quantityAvailable * i.averageCost) "
            + "FROM Inventory i WHERE i.branch.shop.id = :shopId "
            + "AND (:branchId IS NULL OR i.branch.id = :branchId) "
            + "AND i.quantityAvailable > 0 ORDER BY i.product.name ASC")
    List<Object[]> inventoryValuation(@Param("shopId") UUID shopId, @Param("branchId") UUID branchId);

    /** Count products with stock > 0 but no sales since a given cutoff date (slow-moving). */
    @Query("SELECT COUNT(DISTINCT i.product.id) FROM Inventory i "
            + "WHERE i.branch.shop.id = :shopId AND i.quantityAvailable > 0 "
            + "AND i.product.id NOT IN ("
            + "  SELECT DISTINCT si.product.id FROM SaleItem si "
            + "  WHERE si.sale.shop.id = :shopId AND si.sale.billDate >= :since)")
    long countSlowMovingByShop(@Param("shopId") UUID shopId, @Param("since") java.time.LocalDate since);
}