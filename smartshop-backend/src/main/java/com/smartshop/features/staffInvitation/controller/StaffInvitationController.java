package com.smartshop.features.staffInvitation.controller;

import com.smartshop.features.staffInvitation.dto.StaffInvitationApproveRequest;
import com.smartshop.features.staffInvitation.dto.StaffInvitationRejectRequest;
import com.smartshop.features.staffInvitation.dto.StaffInvitationResponse;
import com.smartshop.features.staffInvitation.dto.StaffJoinRequest;
import com.smartshop.features.staffInvitation.service.StaffInvitationService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff-invitations")
@RequiredArgsConstructor
@Tag(name = "Staff Invitations")
public class StaffInvitationController {

    private final StaffInvitationService staffInvitationService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<StaffInvitationResponse>> join(
            @Valid @RequestBody StaffJoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Join request submitted successfully",
                        staffInvitationService.join(request)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffInvitationResponse>>> listPending(
            @RequestParam UUID shopId) {
        return ResponseEntity.ok(ApiResponse.success(staffInvitationService.listPending(shopId)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<StaffInvitationResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody StaffInvitationApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Join request approved successfully",
                staffInvitationService.approve(id, request)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<StaffInvitationResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody StaffInvitationRejectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Join request rejected",
                staffInvitationService.reject(id, request)));
    }
}
