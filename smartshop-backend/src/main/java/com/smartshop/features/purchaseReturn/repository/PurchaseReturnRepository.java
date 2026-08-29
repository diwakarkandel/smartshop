package com.smartshop.features.purchaseReturn.repository;

import com.smartshop.features.purchaseReturn.entity.PurchaseReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID>, JpaSpecificationExecutor<PurchaseReturn> {

    boolean existsByReturnNumber(String returnNumber);

    @Query("SELECT COUNT(r) FROM PurchaseReturn r WHERE r.branch.code = :branchCode AND r.returnDate = :date")
    long countByBranchCodeAndDate(@Param("branchCode") String branchCode, @Param("date") LocalDate date);
}