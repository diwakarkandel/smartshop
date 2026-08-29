package com.smartshop.features.shop.service;

import com.smartshop.features.shop.dto.InviteCodeResponse;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.entity.ShopInviteCode;
import com.smartshop.features.shop.repository.ShopInviteCodeRepository;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.ShopAdminGuard;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopInviteCodeService {

    private static final String INVITE_CODE_PREFIX = "SHOP-";
    private static final int INVITE_CODE_LENGTH = 5;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ShopInviteCodeRepository shopInviteCodeRepository;
    private final ShopRepository shopRepository;
    private final ShopAdminGuard shopAdminGuard;

    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(UUID shopId) {
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), shopId);
        ShopInviteCode inviteCode = shopInviteCodeRepository.findFirstByShopIdAndIsActiveTrueOrderByIdDesc(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("No active invite code found for this shop"));
        return toResponse(inviteCode);
    }

    @Transactional
    public InviteCodeResponse regenerateInviteCode(UUID shopId) {
        log.info("Regenerating invite code for shop: {}", shopId);
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));

        List<ShopInviteCode> activeCodes = shopInviteCodeRepository.findByShopIdAndIsActiveTrue(shopId);
        activeCodes.forEach(code -> code.setIsActive(false));
        shopInviteCodeRepository.saveAll(activeCodes);

        ShopInviteCode created = createInviteCode(shop);
        log.info("New invite code generated for shop: {}", shopId);
        return toResponse(created);
    }

    public ShopInviteCode createInviteCode(Shop shop) {
        ShopInviteCode inviteCode = new ShopInviteCode();
        inviteCode.setShop(shop);
        inviteCode.setCode(generateUniqueCode());
        inviteCode.setIsActive(true);
        return shopInviteCodeRepository.save(inviteCode);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(INVITE_CODE_PREFIX);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                builder.append(INVITE_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = builder.toString();
        } while (shopInviteCodeRepository.existsByCode(code));
        return code;
    }

    private InviteCodeResponse toResponse(ShopInviteCode inviteCode) {
        return InviteCodeResponse.builder()
                .shopId(inviteCode.getShop().getId())
                .code(inviteCode.getCode())
                .isActive(inviteCode.getIsActive())
                .createdAt(inviteCode.getCreatedAt())
                .build();
    }
}
