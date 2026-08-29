package com.smartshop.features.staffInvitation.dto;

import com.smartshop.shared.enumeration.InvitationStatus;
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
public class StaffInvitationResponse {

    private UUID id;
    private UUID userId;
    private String applicantName;
    private String applicantEmail;
    private UUID shopId;
    private String shopName;
    private UUID branchId;
    private String inviteCode;
    private String requestedRole;
    private InvitationStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
