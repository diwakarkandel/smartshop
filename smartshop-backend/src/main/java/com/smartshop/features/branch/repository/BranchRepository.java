package com.smartshop.features.branch.repository;

import com.smartshop.features.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findByShopIdOrderByNameAsc(UUID shopId);

    boolean existsByShopIdAndCode(UUID shopId, String code);

    boolean existsByShopIdAndIsMainBranchTrue(UUID shopId);

    Optional<Branch> findFirstByShopIdAndIsMainBranchTrue(UUID shopId);
}