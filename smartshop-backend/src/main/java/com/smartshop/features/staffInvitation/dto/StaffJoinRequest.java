package com.smartshop.features.staffInvitation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffJoinRequest {

    @NotBlank(message = "Invite code is required")
    private String inviteCode;

    @NotBlank(message = "Requested role is required")
    private String requestedRole;
}
