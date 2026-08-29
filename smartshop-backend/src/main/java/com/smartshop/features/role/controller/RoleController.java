package com.smartshop.features.role.controller;

import com.smartshop.features.role.dto.RoleResponse;
import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        List<RoleResponse> roles = roleRepository.findAll().stream()
                .map(r -> RoleResponse.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}