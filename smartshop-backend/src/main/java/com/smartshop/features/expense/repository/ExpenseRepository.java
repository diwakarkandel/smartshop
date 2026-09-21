package com.smartshop.features.expense.repository;

import com.smartshop.features.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId AND e.expenseDate = :date")
    BigDecimal sumByShopAndDate(@Param("shopId") UUID shopId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.shop.id = :shopId "
            + "AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumByShopAndRange(@Param("shopId") UUID shopId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Expense breakdown by category (uses legacy string field). Returns [categoryName, total]. */
    @Query("SELECT COALESCE(e.category, 'Uncategorized'), COALESCE(SUM(e.amount), 0) "
            + "FROM Expense e WHERE e.shop.id = :shopId AND e.expenseDate BETWEEN :from AND :to "
            + "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategoryAndRange(@Param("shopId") UUID shopId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}