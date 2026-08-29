package com.smartshop.features.staffInvitation.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.entity.ShopInviteCode;
import com.smartshop.features.shop.repository.ShopInviteCodeRepository;
import com.smartshop.features.staffInvitation.dto.StaffInvitationApproveRequest;
import com.smartshop.features.staffInvitation.dto.StaffInvitationRejectRequest;
import com.smartshop.features.staffInvitation.dto.StaffInvitationResponse;
import com.smartshop.features.staffInvitation.dto.StaffJoinRequest;
import com.smartshop.features.staffInvitation.entity.StaffInvitation;
import com.smartshop.features.staffInvitation.mapper.StaffInvitationMapper;
import com.smartshop.features.staffInvitation.repository.StaffInvitationRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.ShopAdminGuard;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.email.EmailService;
import com.smartshop.shared.enumeration.InvitationStatus;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffInvitationService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            AppConstants.ROLE_CASHIER, AppConstants.ROLE_MANAGER, AppConstants.ROLE_INVENTORY_STAFF);

    private final StaffInvitationRepository staffInvitationRepository;
    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final ShopInviteCodeRepository shopInviteCodeRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final ShopAdminGuard shopAdminGuard;
    private final StaffInvitationMapper staffInvitationMapper;
    private final EmailService emailService;

    @Transactional
    public StaffInvitationResponse join(StaffJoinRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        log.info("User {} requesting to join shop with invite code: {}", userId, request.getInviteCode());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ShopInviteCode inviteCodeRecord = shopInviteCodeRepository
                .findByCodeAndIsActiveTrue(request.getInviteCode().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or inactive invite code"));
        Shop shop = inviteCodeRecord.getShop();

        String requestedRole = request.getRequestedRole().trim().toUpperCase();
        if (!ASSIGNABLE_ROLES.contains(requestedRole)) {
            throw new BadRequestException("Requested role must be one of: CASHIER, MANAGER, INVENTORY_STAFF");
        }

        boolean alreadyMember = userBranchRoleRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .anyMatch(grant -> grant.getShop() != null && shop.getId().equals(grant.getShop().getId()));
        if (alreadyMember) {
            throw new DuplicateResourceException("You are already associated with this shop");
        }
        if (staffInvitationRepository.existsByUserIdAndShopIdAndStatus(
                userId, shop.getId(), InvitationStatus.PENDING)) {
            throw new DuplicateResourceException("You already have a pending join request for this shop");
        }

        StaffInvitation invitation = new StaffInvitation();
        invitation.setUser(user);
        invitation.setShop(shop);
        invitation.setBranch(null);
        invitation.setInviteCode(inviteCodeRecord.getCode());
        invitation.setRequestedRole(requestedRole);
        invitation.setStatus(InvitationStatus.PENDING);
        StaffInvitation saved = staffInvitationRepository.save(invitation);
        log.info("Staff invitation request created with id: {}", saved.getId());
        return staffInvitationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffInvitationResponse> listPending(UUID shopId) {
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), shopId);
        return staffInvitationRepository
                .findByShopIdAndStatusOrderByCreatedAtDesc(shopId, InvitationStatus.PENDING)
                .stream()
                .map(staffInvitationMapper::toResponse)
                .toList();
    }

    @Transactional
    public StaffInvitationResponse approve(UUID id, StaffInvitationApproveRequest request) {
        log.info("Approving staff invitation request: {}", id);
        StaffInvitation invitation = getRequired(id);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), invitation.getShop().getId());
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Only pending join requests can be approved");
        }

        String roleName = request.getRole().trim().toUpperCase();
        if (!ASSIGNABLE_ROLES.contains(roleName)) {
            throw new BadRequestException("Role must be one of: CASHIER, MANAGER, INVENTORY_STAFF");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
            if (!branch.getShop().getId().equals(invitation.getShop().getId())) {
                throw new BadRequestException("The selected branch does not belong to this shop");
            }
        }

        UserBranchRole grant = new UserBranchRole();
        grant.setUser(invitation.getUser());
        grant.setShop(invitation.getShop());
        grant.setBranch(branch);
        grant.setRole(role);
        grant.setIsActive(true);
        userBranchRoleRepository.save(grant);

        invitation.setStatus(InvitationStatus.APPROVED);
        invitation.setBranch(branch);
        StaffInvitation saved = staffInvitationRepository.save(invitation);

        try {
            emailService.sendStaffApprovalEmail(saved.getUser().getEmail(), saved.getUser().getFullName(),
                    saved.getShop().getName(), role.getName());
        } catch (Exception e) {
            log.warn("Failed to send staff approval email: {}", e.getMessage());
        }
        log.info("Staff invitation {} approved with role {}", id, role.getName());
        return staffInvitationMapper.toResponse(saved);
    }

    @Transactional
    public StaffInvitationResponse reject(UUID id, StaffInvitationRejectRequest request) {
        log.info("Rejecting staff invitation request: {}", id);
        StaffInvitation invitation = getRequired(id);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), invitation.getShop().getId());
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Only pending join requests can be rejected");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRejectionReason(request.getRejectionReason().trim());
        StaffInvitation saved = staffInvitationRepository.save(invitation);

        try {
            emailService.sendStaffRejectionEmail(saved.getUser().getEmail(), saved.getUser().getFullName(),
                    saved.getShop().getName(), saved.getRejectionReason());
        } catch (Exception e) {
            log.warn("Failed to send staff rejection email: {}", e.getMessage());
        }
        log.info("Staff invitation {} rejected", id);
        return staffInvitationMapper.toResponse(saved);
    }

    private StaffInvitation getRequired(UUID id) {
        return staffInvitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff invitation", id));
    }
}
