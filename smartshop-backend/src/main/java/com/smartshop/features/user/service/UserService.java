package com.smartshop.features.user.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.user.dto.UpdateProfileRequest;
import com.smartshop.features.user.dto.UserResponse;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.UserPrincipal;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.UserStatus;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Pageable pageable) {
        UserPrincipal caller = SecurityUtils.currentUser();

        if (caller.hasRole(AppConstants.ROLE_SUPER_ADMIN)) {
            // SUPER_ADMIN: return all users, optionally filtered by search term
            Page<User> page;
            if (search == null || search.isBlank()) {
                page = userRepository.findAll(pageable);
            } else {
                String term = "%" + search.toLowerCase() + "%";
                page = userRepository.findAll((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("firstName")), term),
                        cb.like(cb.lower(root.get("lastName")), term),
                        cb.like(cb.lower(root.get("email")), term)), pageable);
            }
            return page.map(this::toResponse);
        }

        // SHOP_ADMIN / STAFF: scope to users belonging to their own shop(s).
        // Shop IDs are derived from the authenticated principal — never from a client param.
        Set<UUID> callerShopIds = caller.getBranchRoleGrants().stream()
                .filter(g -> g.shopId() != null)
                .map(g -> g.shopId())
                .collect(Collectors.toSet());

        if (callerShopIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Collect all user IDs that belong to ANY of the caller's shops
        Set<UUID> allowedUserIds = callerShopIds.stream()
                .flatMap(shopId -> userBranchRoleRepository.findActiveUserIdsByShopId(shopId).stream())
                .collect(Collectors.toSet());

        if (allowedUserIds.isEmpty()) {
            return Page.empty(pageable);
        }

        final String finalTerm = (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";
        Page<User> page = userRepository.findAll((root, query, cb) -> {
            var inAllowed = root.get("id").in(allowedUserIds);
            if (finalTerm == null) {
                return inAllowed;
            }
            return cb.and(inAllowed, cb.or(
                    cb.like(cb.lower(root.get("firstName")), finalTerm),
                    cb.like(cb.lower(root.get("lastName")), finalTerm),
                    cb.like(cb.lower(root.get("email")), finalTerm)));
        }, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        UserPrincipal caller = SecurityUtils.currentUser();

        if (caller.hasRole(AppConstants.ROLE_SUPER_ADMIN)) {
            return toResponse(getEntity(id));
        }

        // SHOP_ADMIN / STAFF: confirm the requested user belongs to their shop.
        // Return 404 (not 403) so we do not leak whether the user exists on the platform.
        Set<UUID> callerShopIds = caller.getBranchRoleGrants().stream()
                .filter(g -> g.shopId() != null)
                .map(g -> g.shopId())
                .collect(Collectors.toSet());

        boolean userInShop = callerShopIds.stream()
                .anyMatch(shopId -> userBranchRoleRepository
                        .findActiveUserIdsByShopId(shopId).contains(id));

        if (!userInShop) {
            throw new ResourceNotFoundException("User", id);
        }
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return toResponse(getEntity(SecurityUtils.currentUserId()));
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", SecurityUtils.currentUserId());
        User user = getEntity(SecurityUtils.currentUserId());
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl().trim());
        }
        User saved = userRepository.save(user);
        log.info("User profile updated for user id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatus status) {
        log.info("Updating user {} status to {}", id, status);
        User user = getEntity(id);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        User saved = userRepository.save(user);
        auditService.log("STATUS_CHANGE", "User", saved.getId().toString(),
                oldStatus == null ? null : oldStatus.name(), status.name());
        log.info("User {} status updated to {}", id, status);
        return toResponse(saved);
    }

    public User getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        List<UserBranchRole> assignments = userBranchRoleRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<UserResponse.UserBranchRoleRef> refs = assignments.stream()
                .map(a -> UserResponse.UserBranchRoleRef.builder()
                        .branchId(a.getBranch() == null ? null : a.getBranch().getId())
                        .branchName(a.getBranch() == null ? null : a.getBranch().getName())
                        .shopId(a.getShop() == null ? null : a.getShop().getId())
                        .role(a.getRole().getName())
                        .build())
                .toList();
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .branchRoles(refs)
                .createdAt(user.getCreatedAt())
                .build();
    }
}