package com.smartshop.features.category.repository;

import com.smartshop.features.category.entity.Category;
import com.smartshop.shared.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    List<Category> findByShopIdOrderByNameAsc(UUID shopId);

    List<Category> findByShopIdAndParentIsNullOrderByNameAsc(UUID shopId);

    long countByParentId(UUID parentId);

    boolean existsByShopIdAndCode(UUID shopId, String code);

    boolean existsByIdAndStatus(UUID id, Status status);
}