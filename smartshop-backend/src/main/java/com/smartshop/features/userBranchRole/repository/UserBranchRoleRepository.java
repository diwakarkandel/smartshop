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

    /**
     * Returns all distinct user IDs that have an active role assignment in a given shop
     * (either directly on the shop or via a branch that belongs to the shop).
     * Used by UserService to scope user listings for SHOP_ADMIN callers.
     */
    @Query("""
            SELECT DISTINCT ubr.user.id FROM UserBranchRole ubr
            WHERE ubr.isActive = true
              AND (ubr.shop.id = :shopId OR ubr.branch.shop.id = :shopId)
            """)
    List<UUID> findActiveUserIdsByShopId(@Param("shopId") UUID shopId);

    /**
     * Distinct email addresses of all active staff of a shop (directly on the shop
     * or via a branch of it). Used to notify the team of customer-record changes.
     */
    @Query("""
            SELECT DISTINCT ubr.user.email FROM UserBranchRole ubr
            WHERE ubr.isActive = true
              AND ubr.user.email IS NOT NULL
              AND (ubr.shop.id = :shopId OR ubr.branch.shop.id = :shopId)
            """)
    List<String> findActiveStaffEmailsByShopId(@Param("shopId") UUID shopId);
}