package com.smartshop.features.userBranchRole.controller;

import com.smartshop.features.userBranchRole.dto.UserBranchRoleRequest;
import com.smartshop.features.userBranchRole.dto.UserBranchRoleResponse;
import com.smartshop.features.userBranchRole.service.UserBranchRoleService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/user-branch-roles")
@RequiredArgsConstructor
@Tag(name = "User Branch Roles")
public class UserBranchRoleController {

    private final UserBranchRoleService userBranchRoleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserBranchRoleResponse>>> listByUser(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userBranchRoleService.listByUser(userId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<UserBranchRoleResponse>> assign(@Valid @RequestBody UserBranchRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role assigned successfully", userBranchRoleService.assign(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id) {
        userBranchRoleService.revoke(id);
        return ResponseEntity.ok(ApiResponse.success("Role assignment revoked", null));
    }
}