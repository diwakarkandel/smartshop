package com.smartshop.features.staffInvitation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitationApproveRequest {

    private UUID branchId;

    @NotBlank(message = "Role is required")
    private String role;
}
