package com.smartshop.features.saleReturn.repository;

import com.smartshop.features.saleReturn.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID>, JpaSpecificationExecutor<SaleReturn> {

    boolean existsByReturnNumber(String returnNumber);

    @Query("SELECT COUNT(r) FROM SaleReturn r WHERE r.branch.code = :branchCode AND r.returnDate = :date")
    long countByBranchCodeAndDate(@Param("branchCode") String branchCode, @Param("date") LocalDate date);
}