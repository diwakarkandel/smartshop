package com.smartshop.features.sale.repository;

import com.smartshop.features.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    boolean existsByProductId(UUID productId);

    List<SaleItem> findBySaleId(UUID saleId);

    @Query("SELECT COALESCE(SUM(i.quantity * "
            + "CASE WHEN i.unitCostAtSale > 0 THEN i.unitCostAtSale ELSE inv.averageCost END), 0) FROM SaleItem i "
            + "JOIN i.sale s JOIN Inventory inv ON inv.branch.id = s.branch.id AND inv.product.id = i.product.id "
            + "WHERE s.shop.id = :shopId AND s.billDate BETWEEN :from AND :to")
    BigDecimal sumCogsByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT i.product.id, i.product.name, i.product.sku, SUM(i.quantity), SUM(i.lineTotal), "
            + "COALESCE(SUM(i.lineProfit), 0), COALESCE(SUM(i.quantity * i.unitCostAtSale), 0) "
            + "FROM SaleItem i WHERE i.sale.shop.id = :shopId AND i.sale.billDate BETWEEN :from AND :to "
            + "GROUP BY i.product.id, i.product.name, i.product.sku")
    List<Object[]> productPerformanceByRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT i.product.id, COALESCE(SUM(i.quantity), 0) "
            + "FROM SaleItem i WHERE i.sale.shop.id = :shopId AND i.sale.billDate BETWEEN :from AND :to "
            + "GROUP BY i.product.id")
    List<Object[]> quantityByProductAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT i.product.id, MAX(i.sale.billDate) "
            + "FROM SaleItem i WHERE i.sale.shop.id = :shopId AND i.sale.billDate < :cutoff "
            + "GROUP BY i.product.id")
    List<Object[]> lastSaleDatePerProduct(@Param("shopId") UUID shopId, @Param("cutoff") LocalDate cutoff);

    /**
     * Profit breakdown grouped by product category.
     * Returns: [categoryId, categoryName, totalRevenue, totalCogs, totalProfit, totalQty]
     */
    @Query("SELECT i.product.category.id, i.product.category.name, "
            + "COALESCE(SUM(i.lineTotal), 0), COALESCE(SUM(i.quantity * i.unitCostAtSale), 0), "
            + "COALESCE(SUM(i.lineProfit), 0), COALESCE(SUM(i.quantity), 0) "
            + "FROM SaleItem i WHERE i.sale.shop.id = :shopId AND i.sale.billDate BETWEEN :from AND :to "
            + "GROUP BY i.product.category.id, i.product.category.name "
            + "ORDER BY SUM(i.lineProfit) DESC")
    List<Object[]> profitByCategoryAndRange(@Param("shopId") UUID shopId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}