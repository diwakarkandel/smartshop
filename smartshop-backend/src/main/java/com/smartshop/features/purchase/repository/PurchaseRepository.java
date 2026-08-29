package com.smartshop.features.purchase.repository;

import com.smartshop.features.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID>, JpaSpecificationExecutor<Purchase> {

    boolean existsByPurchaseNumber(String purchaseNumber);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.branch.code = :branchCode AND p.purchaseDate = :date")
    long countByBranchCodeAndDate(@Param("branchCode") String branchCode, @Param("date") LocalDate date);

    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.shop.id = :shopId AND p.purchaseDate = :date")
    BigDecimal sumTotalByShopAndDate(@Param("shopId") UUID shopId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.shop.id = :shopId "
            + "AND p.purchaseDate BETWEEN :from AND :to")
    BigDecimal sumTotalByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}