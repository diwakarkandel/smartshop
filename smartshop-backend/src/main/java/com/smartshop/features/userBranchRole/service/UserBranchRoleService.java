package com.smartshop.features.userBranchRole.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.dto.UserBranchRoleRequest;
import com.smartshop.features.userBranchRole.dto.UserBranchRoleResponse;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ForbiddenException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBranchRoleService {

    private final UserBranchRoleRepository userBranchRoleRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    @Transactional
    public UserBranchRoleResponse assign(UserBranchRoleRequest request) {
        log.info("Assigning role {} to user {} in shop {}", request.getRoleId(), request.getUserId(), request.getShopId());
        UUID currentUserId = SecurityUtils.currentUserId();
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", request.getRoleId()));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
            if (!branch.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Branch does not belong to the selected shop");
            }
        }

        assertAssignmentAllowed(currentUserId, shop, role);

        if (userBranchRoleRepository.existsByUserIdAndShopIdAndBranchIdAndRoleId(
                request.getUserId(), shop.getId(), request.getBranchId(), request.getRoleId())) {
            throw new DuplicateResourceException("User already has this role assignment");
        }

        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setShop(shop);
        assignment.setBranch(branch);
        assignment.setRole(role);
        assignment.setIsActive(true);
        UserBranchRole saved = userBranchRoleRepository.save(assignment);

        auditService.log("ROLE_ASSIGN", "UserBranchRole", saved.getId().toString(), null,
                "User: " + user.getEmail() + ", Role: " + role.getName() + ", Shop: " + shop.getName());
        log.info("Role {} assigned to user {} successfully", role.getName(), user.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public void revoke(UUID id) {
        log.info("Revoking role assignment: {}", id);
        UserBranchRole assignment = userBranchRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserBranchRole", id));
        UUID currentUserId = SecurityUtils.currentUserId();
        branchScopeGuard.requireShopAccess(currentUserId, assignment.getShop().getId());
        boolean isSuperAdmin = SecurityUtils.currentUser().hasRole(AppConstants.ROLE_SUPER_ADMIN);
        boolean targetIsSuperAdmin = AppConstants.ROLE_SUPER_ADMIN.equals(assignment.getRole().getName());
        if (!isSuperAdmin && targetIsSuperAdmin) {
            throw new ForbiddenException("Only SUPER_ADMIN can revoke a SUPER_ADMIN role");
        }
        assignment.setIsActive(false);
        userBranchRoleRepository.save(assignment);

        auditService.log("ROLE_REVOKE", "UserBranchRole", assignment.getId().toString(),
                "Role: " + assignment.getRole().getName(), "REVOKED");
        log.info("Role assignment {} revoked successfully", id);
    }

    @Transactional(readOnly = true)
    public List<UserBranchRoleResponse> listByUser(UUID userId) {
        return userBranchRoleRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertAssignmentAllowed(UUID currentUserId, Shop shop, Role role) {
        boolean isSuperAdmin = SecurityUtils.currentUser().hasRole(AppConstants.ROLE_SUPER_ADMIN);
        if (isSuperAdmin) {
            return;
        }
        boolean isShopAdmin = SecurityUtils.currentUser().hasRole(AppConstants.ROLE_SHOP_ADMIN);
        if (!isShopAdmin) {
            throw new ForbiddenException("Only SUPER_ADMIN or SHOP_ADMIN can assign roles");
        }
        if (AppConstants.ROLE_SUPER_ADMIN.equals(role.getName())) {
            throw new ForbiddenException("SHOP_ADMIN cannot assign the SUPER_ADMIN role");
        }
        branchScopeGuard.requireShopAccess(currentUserId, shop.getId());
    }

    private UserBranchRoleResponse toResponse(UserBranchRole a) {
        return UserBranchRoleResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .userFullName(a.getUser().getFullName())
                .shopId(a.getShop().getId())
                .shopName(a.getShop().getName())
                .branchId(a.getBranch() == null ? null : a.getBranch().getId())
                .branchName(a.getBranch() == null ? null : a.getBranch().getName())
                .roleId(a.getRole().getId())
                .roleName(a.getRole().getName())
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .build();
    }
}