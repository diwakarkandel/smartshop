package com.smartshop.features.userBranchRole.repository;

import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, UUID> {

    @Query("""
            SELECT DISTINCT ubr FROM UserBranchRole ubr
            LEFT JOIN FETCH ubr.role
            LEFT JOIN FETCH ubr.shop
            LEFT JOIN FETCH ubr.branch b
            LEFT JOIN FETCH b.shop
            WHERE ubr.user.id = :userId AND ubr.isActive = true
            """)
    List<UserBranchRole> findByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT ubr FROM UserBranchRole ubr
            LEFT JOIN FETCH ubr.role
            LEFT JOIN FETCH ubr.shop
            LEFT JOIN FETCH ubr.branch b
            LEFT JOIN FETCH b.shop
            WHERE ubr.user.id = :userId
            ORDER BY ubr.createdAt ASC
            """)
    List<UserBranchRole> findByUserIdOrderByCreatedAtAsc(@Param("userId") UUID userId);

    boolean existsByUserIdAndShopIdAndBranchIdAndRoleId(UUID userId, UUID shopId, UUID branchId, UUID roleId);

    @Query("""
            SELECT ubr FROM UserBranchRole ubr
            WHERE ubr.user.id = :userId AND ubr.isActive = true
              AND ubr.branch.id = :branchId
            """)
    List<UserBranchRole> findActiveByUserAndBranch(@Param("userId") UUID userId, @Param("branchId") UUID branchId);
}