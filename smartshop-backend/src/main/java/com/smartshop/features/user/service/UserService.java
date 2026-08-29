package com.smartshop.features.user.service;

import com.smartshop.features.user.dto.UpdateProfileRequest;
import com.smartshop.features.user.dto.UserResponse;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.UserStatus;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
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
        user.setStatus(status);
        User saved = userRepository.save(user);
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
                .branchRoles(refs)
                .createdAt(user.getCreatedAt())
                .build();
    }
}