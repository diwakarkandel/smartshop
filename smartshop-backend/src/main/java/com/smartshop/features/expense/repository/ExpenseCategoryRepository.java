package com.smartshop.features.expense.repository;

import com.smartshop.features.expense.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    /** Returns global (shop=null) + shop-specific categories, sorted by name. */
    @Query("SELECT c FROM ExpenseCategory c WHERE c.shop IS NULL OR c.shop.id = :shopId ORDER BY c.name ASC")
    List<ExpenseCategory> findByShopIdOrGlobal(@Param("shopId") UUID shopId);

    /** All global categories (seed data), ordered by name. */
    List<ExpenseCategory> findAllByOrderByNameAsc();

    boolean existsByShopIdAndNameIgnoreCase(UUID shopId, String name);

    boolean existsByShopIsNullAndNameIgnoreCase(String name);
}