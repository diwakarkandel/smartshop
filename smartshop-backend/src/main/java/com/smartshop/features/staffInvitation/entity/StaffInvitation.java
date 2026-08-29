package com.smartshop.features.staffInvitation.entity;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.user.entity.User;
import com.smartshop.shared.entity.BaseEntity;
import com.smartshop.shared.enumeration.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;
}
