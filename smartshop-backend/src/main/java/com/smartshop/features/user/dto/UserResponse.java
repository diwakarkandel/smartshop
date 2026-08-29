package com.smartshop.features.user.dto;

import com.smartshop.shared.enumeration.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private UserStatus status;
    private List<UserBranchRoleRef> branchRoles;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBranchRoleRef {
        private UUID branchId;
        private String branchName;
        private UUID shopId;
        private String role;
    }
}