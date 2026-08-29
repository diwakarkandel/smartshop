package com.smartshop.features.sale.repository;

import com.smartshop.features.adminDashboard.dto.ShopSalesProjection;
import com.smartshop.features.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    boolean existsByShopIdAndInvoiceNumber(UUID shopId, String invoiceNumber);

    @Query("""
            SELECT s.shop.id AS shopId, s.shop.name AS shopName, SUM(s.totalAmount) AS totalSales
            FROM Sale s
            GROUP BY s.shop.id, s.shop.name
            ORDER BY SUM(s.totalAmount) DESC
            """)
    List<ShopSalesProjection> aggregateSalesByShop();

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.branch.code = :branchCode AND s.billDate = :date")
    long countByBranchCodeAndDate(@Param("branchCode") String branchCode, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.shop.id = :shopId AND s.billDate = :date")
    BigDecimal sumTotalByShopAndDate(@Param("shopId") UUID shopId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shop.id = :shopId AND s.billDate = :date")
    long countByShopAndDate(@Param("shopId") UUID shopId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.shop.id = :shopId "
            + "AND s.billDate BETWEEN :from AND :to")
    long countByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.shop.id = :shopId "
            + "AND s.billDate BETWEEN :from AND :to")
    BigDecimal sumTotalByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.vatAmount), 0) FROM Sale s WHERE s.shop.id = :shopId "
            + "AND s.billDate BETWEEN :from AND :to")
    BigDecimal sumVatByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.discountAmount), 0) FROM Sale s WHERE s.shop.id = :shopId "
            + "AND s.billDate BETWEEN :from AND :to")
    BigDecimal sumDiscountByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}