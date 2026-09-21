package com.smartshop.security;

import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopAdminGuard {

    private final UserBranchRoleRepository userBranchRoleRepository;

    public void requireShopAdmin(UUID userId, UUID shopId) {
        List<UserBranchRole> grants = userBranchRoleRepository.findByUserIdAndIsActiveTrue(userId);

        // SUPER_ADMIN has platform-wide access and bypasses all shop-ownership checks.
        boolean isSuperAdmin = grants.stream()
                .anyMatch(grant -> AppConstants.ROLE_SUPER_ADMIN.equals(grant.getRole().getName()));
        if (isSuperAdmin) {
            return;
        }

        boolean isShopAdminOfShop = grants.stream()
                .anyMatch(grant -> grant.getBranch() == null
                        && grant.getShop() != null
                        && shopId.equals(grant.getShop().getId())
                        && AppConstants.ROLE_SHOP_ADMIN.equals(grant.getRole().getName()));
        if (!isShopAdminOfShop) {
            throw new ForbiddenException("You are not the administrator of this shop");
        }
    }
}
