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

    @Query("SELECT COALESCE(SUM(i.quantity * inv.averageCost), 0) FROM SaleItem i "
            + "JOIN i.sale s JOIN Inventory inv ON inv.branch.id = s.branch.id AND inv.product.id = i.product.id "
            + "WHERE s.shop.id = :shopId AND s.billDate BETWEEN :from AND :to")
    BigDecimal sumCogsByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT i.product.id, i.product.name, i.product.sku, SUM(i.quantity), SUM(i.lineTotal) "
            + "FROM SaleItem i WHERE i.sale.shop.id = :shopId AND i.sale.billDate BETWEEN :from AND :to "
            + "GROUP BY i.product.id, i.product.name, i.product.sku ORDER BY SUM(i.quantity) DESC")
    List<Object[]> topProductsByRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}