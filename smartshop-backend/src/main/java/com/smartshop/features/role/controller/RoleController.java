package com.smartshop.features.role.controller;

import com.smartshop.features.role.dto.RoleResponse;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles")
public class RoleController {

    private final RoleRepository roleRepository;

    /**
     * SUPER_ADMIN: receives all roles (including SUPER_ADMIN itself).
     * SHOP_ADMIN:  receives only roles assignable to shop staff —
     *              SUPER_ADMIN role is hidden so it cannot be mistakenly assigned.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        boolean isSuperAdmin = SecurityUtils.currentUser().hasRole(AppConstants.ROLE_SUPER_ADMIN);

        List<RoleResponse> roles = roleRepository.findAll().stream()
                // SHOP_ADMIN must NOT see the SUPER_ADMIN role — prevents accidental assignment
                .filter(r -> isSuperAdmin || !AppConstants.ROLE_SUPER_ADMIN.equals(r.getName()))
                .map(r -> RoleResponse.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}