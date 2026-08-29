package com.smartshop.features.staffInvitation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitationRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
