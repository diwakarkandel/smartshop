package com.smartshop.features.userBranchRole.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBranchRoleRequest {

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    private UUID branchId;

    @NotNull(message = "Role id is required")
    private UUID roleId;
}