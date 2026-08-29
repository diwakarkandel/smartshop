package com.smartshop.features.userBranchRole.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBranchRoleResponse {

    private UUID id;
    private UUID userId;
    private String userFullName;
    private UUID shopId;
    private String shopName;
    private UUID branchId;
    private String branchName;
    private UUID roleId;
    private String roleName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}