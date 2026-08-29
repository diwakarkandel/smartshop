package com.smartshop.features.shop.controller;

import com.smartshop.features.shop.dto.InviteCodeResponse;
import com.smartshop.features.shop.service.ShopInviteCodeService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops/{shopId}/invite-code")
@RequiredArgsConstructor
@Tag(name = "Shop Invite Codes")
public class ShopInviteCodeController {

    private final ShopInviteCodeService shopInviteCodeService;

    @GetMapping
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> getInviteCode(@PathVariable UUID shopId) {
        return ResponseEntity.ok(ApiResponse.success(shopInviteCodeService.getInviteCode(shopId)));
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> regenerateInviteCode(@PathVariable UUID shopId) {
        return ResponseEntity.ok(ApiResponse.success("New invite code generated",
                shopInviteCodeService.regenerateInviteCode(shopId)));
    }
}
