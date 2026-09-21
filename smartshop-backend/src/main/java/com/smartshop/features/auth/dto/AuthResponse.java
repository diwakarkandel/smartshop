package com.smartshop.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UUID userId;
    private String email;
    private String fullName;
    private List<String> roles;
    private List<BranchRoleResponse> branchRoles;
    private String profileImageUrl;
    private boolean emailVerified;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchRoleResponse {
        private UUID branchId;
        private String branchName;
        private UUID shopId;
        private String role;
    }
}