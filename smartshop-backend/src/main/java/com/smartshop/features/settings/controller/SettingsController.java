package com.smartshop.features.settings.controller;

import com.smartshop.features.settings.dto.SettingsRequest;
import com.smartshop.features.settings.dto.SettingsResponse;
import com.smartshop.features.settings.entity.Setting;
import com.smartshop.features.settings.service.SettingsService;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.ResourceNotFoundException;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final BranchScopeGuard branchScopeGuard;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<List<SettingsResponse>>> list(@RequestParam UUID shopId) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        List<SettingsResponse> data = settingsService.listSettings(shopId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<SettingsResponse>> update(@Valid @RequestBody SettingsRequest request) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        UUID userId = SecurityUtils.currentUserId();
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Setting setting = settingsService.updateSetting(request.getShopId(), request.getKey(),
                request.getValue(), userId);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", toResponse(setting)));
    }

    private SettingsResponse toResponse(Setting setting) {
        return SettingsResponse.builder()
                .id(setting.getId())
                .shopId(setting.getShop().getId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .description(setting.getDescription())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}