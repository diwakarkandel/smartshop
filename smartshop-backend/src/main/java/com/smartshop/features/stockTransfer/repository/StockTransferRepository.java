package com.smartshop.features.stockTransfer.repository;

import com.smartshop.features.stockTransfer.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID>, JpaSpecificationExecutor<StockTransfer> {

    boolean existsByTransferNumber(String transferNumber);

    @Query("SELECT COUNT(t) FROM StockTransfer t WHERE t.fromBranch.code = :branchCode AND CAST(t.createdAt AS date) = :date")
    long countByFromBranchCodeAndDate(@Param("branchCode") String branchCode, @Param("date") LocalDate date);
}