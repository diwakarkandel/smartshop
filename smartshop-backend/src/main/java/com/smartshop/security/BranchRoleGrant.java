package com.smartshop.security;

import java.util.UUID;

public record BranchRoleGrant(UUID branchId, UUID shopId, String roleName) {
    public boolean isShopScoped() {
        return branchId == null;
    }
}