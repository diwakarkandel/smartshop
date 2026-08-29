package com.smartshop.security;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.exception.ForbiddenException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchScopeGuard {

    private final UserBranchRoleRepository userBranchRoleRepository;
    private final BranchRepository branchRepository;

    public void requireBranchAccess(UUID userId, UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        requireBranchAccess(userId, branch);
    }

    public void requireBranchAccess(UUID userId, Branch branch) {
        List<UserBranchRole> grants = userBranchRoleRepository.findByUserIdAndIsActiveTrue(userId);
        boolean isSuperAdmin = grants.stream()
                .anyMatch(g -> AppConstants.ROLE_SUPER_ADMIN.equals(g.getRole().getName()));
        if (isSuperAdmin) {
            return;
        }
        boolean branchGrant = grants.stream()
                .anyMatch(g -> g.getBranch() != null && branch.getId().equals(g.getBranch().getId()));
        if (branchGrant) {
            return;
        }
        boolean shopAdminGrant = grants.stream()
                .anyMatch(g -> g.getBranch() == null
                        && AppConstants.ROLE_SHOP_ADMIN.equals(g.getRole().getName())
                        && branch.getShop().getId().equals(g.getShop() != null ? g.getShop().getId() : null));
        if (shopAdminGrant) {
            return;
        }
        throw new ForbiddenException("You do not have access to this branch");
    }

    public void requireShopAccess(UUID userId, UUID shopId) {
        List<UserBranchRole> grants = userBranchRoleRepository.findByUserIdAndIsActiveTrue(userId);
        boolean isSuperAdmin = grants.stream()
                .anyMatch(g -> AppConstants.ROLE_SUPER_ADMIN.equals(g.getRole().getName()));
        if (isSuperAdmin) {
            return;
        }
        boolean shopGrant = grants.stream()
                .anyMatch(g -> g.getShop() != null && shopId.equals(g.getShop().getId()));
        if (shopGrant) {
            return;
        }
        boolean branchOfShopGrant = grants.stream()
                .anyMatch(g -> g.getBranch() != null
                        && shopId.equals(g.getBranch().getShop().getId()));
        if (branchOfShopGrant) {
            return;
        }
        throw new ForbiddenException("You do not have access to this shop");
    }

    public void requireSuperAdmin(UUID userId) {
        List<UserBranchRole> grants = userBranchRoleRepository.findByUserIdAndIsActiveTrue(userId);
        boolean isSuperAdmin = grants.stream()
                .anyMatch(g -> AppConstants.ROLE_SUPER_ADMIN.equals(g.getRole().getName()));
        if (!isSuperAdmin) {
            throw new ForbiddenException("Only SUPER_ADMIN can perform this action");
        }
    }
}