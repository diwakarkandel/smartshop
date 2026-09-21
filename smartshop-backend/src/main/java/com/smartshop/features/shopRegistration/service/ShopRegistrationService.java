package com.smartshop.features.shopRegistration.service;

import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.branch.service.BranchService;
import com.smartshop.features.settings.service.SettingsService;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.shop.service.ShopInviteCodeService;
import com.smartshop.features.shopRegistration.dto.ShopRegistrationApplyRequest;
import com.smartshop.features.shopRegistration.dto.ShopRegistrationRejectRequest;
import com.smartshop.features.shopRegistration.dto.ShopRegistrationResponse;
import com.smartshop.features.shopRegistration.entity.ShopRegistration;
import com.smartshop.features.shopRegistration.mapper.ShopRegistrationMapper;
import com.smartshop.features.shopRegistration.repository.ShopRegistrationRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.email.EmailService;
import com.smartshop.shared.enumeration.RegistrationStatus;
import com.smartshop.shared.enumeration.ShopStatus;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopRegistrationService {

    private final ShopRegistrationRepository shopRegistrationRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final RoleRepository roleRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final ShopInviteCodeService shopInviteCodeService;
    private final SettingsService settingsService;
    private final BranchService branchService;
    private final ShopRegistrationMapper shopRegistrationMapper;
    private final EmailService emailService;

    @Transactional
    public ShopRegistrationResponse apply(ShopRegistrationApplyRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        log.info("User {} applying for shop registration: {}", userId, request.getShopName());
        boolean hasActiveApplication = shopRegistrationRepository.existsByUserIdAndStatusIn(
                userId, EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));
        if (hasActiveApplication) {
            throw new DuplicateResourceException(
                    "You already have a pending or approved shop registration application");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ShopRegistration registration = new ShopRegistration();
        registration.setUser(user);
        registration.setShopName(request.getShopName().trim());
        registration.setPanVatNumber(request.getPanVatNumber().trim());
        registration.setPanCertificateUrl(request.getPanCertificateUrl().trim());
        registration.setPhone(request.getPhone());
        registration.setAddress(request.getAddress());
        registration.setStatus(RegistrationStatus.PENDING);
        ShopRegistration saved = shopRegistrationRepository.save(registration);
        log.info("Shop registration application submitted with id: {}", saved.getId());
        return shopRegistrationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShopRegistrationResponse getMyApplication() {
        ShopRegistration registration = shopRegistrationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not submitted any shop registration application"));
        return shopRegistrationMapper.toResponse(registration);
    }

    @Transactional(readOnly = true)
    public Page<ShopRegistrationResponse> listApplications(RegistrationStatus status, Pageable pageable) {
        Page<ShopRegistration> page = status == null
                ? shopRegistrationRepository.findAll(pageable)
                : shopRegistrationRepository.findByStatus(status, pageable);
        return page.map(shopRegistrationMapper::toResponse);
    }

    @Transactional
    public ShopRegistrationResponse approve(UUID id) {
        log.info("Approving shop registration application: {}", id);
        ShopRegistration registration = getRequired(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be approved");
        }

        Shop shop = createShopFromRegistration(registration);
        assignShopAdminRole(registration.getUser(), shop);
        shopInviteCodeService.createInviteCode(shop);
        settingsService.ensureDefaultSettings(shop, registration.getUser().getId());
        branchService.createDefaultMainBranch(shop);

        registration.setStatus(RegistrationStatus.APPROVED);
        ShopRegistration saved = shopRegistrationRepository.save(registration);

        try {
            emailService.sendShopApprovalEmail(saved.getUser().getEmail(),
                    saved.getUser().getFullName(), shop.getName());
        } catch (Exception e) {
            log.warn("Failed to send approval email: {}", e.getMessage());
        }
        log.info("Shop registration {} approved and shop {} created", id, shop.getId());
        return shopRegistrationMapper.toResponse(saved);
    }

    @Transactional
    public ShopRegistrationResponse reject(UUID id, ShopRegistrationRejectRequest request) {
        log.info("Rejecting shop registration application: {}", id);
        ShopRegistration registration = getRequired(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be rejected");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setRejectionReason(request.getRejectionReason().trim());
        ShopRegistration saved = shopRegistrationRepository.save(registration);

        try {
            emailService.sendShopRejectionEmail(saved.getUser().getEmail(),
                    saved.getUser().getFullName(), saved.getShopName(), saved.getRejectionReason());
        } catch (Exception e) {
            log.warn("Failed to send rejection email: {}", e.getMessage());
        }
        log.info("Shop registration {} rejected", id);
        return shopRegistrationMapper.toResponse(saved);
    }

    private Shop createShopFromRegistration(ShopRegistration registration) {
        Shop shop = new Shop();
        shop.setName(registration.getShopName());
        shop.setPanVatNumber(registration.getPanVatNumber());
        shop.setPhone(registration.getPhone());
        shop.setAddress(registration.getAddress());
        shop.setEmail(registration.getUser().getEmail());
        shop.setStatus(ShopStatus.ACTIVE);
        try {
            return shopRepository.saveAndFlush(shop);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("A shop with this PAN/VAT number already exists");
        }
    }

    private void assignShopAdminRole(User user, Shop shop) {
        Role shopAdminRole = roleRepository.findByName(AppConstants.ROLE_SHOP_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role", AppConstants.ROLE_SHOP_ADMIN));
        UserBranchRole grant = new UserBranchRole();
        grant.setUser(user);
        grant.setShop(shop);
        grant.setBranch(null);
        grant.setRole(shopAdminRole);
        grant.setIsActive(true);
        userBranchRoleRepository.save(grant);
    }

    private ShopRegistration getRequired(UUID id) {
        return shopRegistrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop registration", id));
    }
}
