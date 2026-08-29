package com.smartshop.features.staffInvitation.repository;

import com.smartshop.features.staffInvitation.entity.StaffInvitation;
import com.smartshop.shared.enumeration.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {

    List<StaffInvitation> findByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, InvitationStatus status);

    boolean existsByUserIdAndShopIdAndStatus(UUID userId, UUID shopId, InvitationStatus status);
}
