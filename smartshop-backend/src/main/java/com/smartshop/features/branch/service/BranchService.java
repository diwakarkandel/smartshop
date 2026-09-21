package com.smartshop.features.branch.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.dto.BranchRequest;
import com.smartshop.features.branch.dto.BranchResponse;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.BranchStatus;
import com.smartshop.shared.exception.DuplicateResourceException;
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
public class BranchService {

    private final BranchRepository branchRepository;
    private final ShopRepository shopRepository;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        log.info("Creating branch with code: {} in shop: {}", request.getCode(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        if (branchRepository.existsByShopIdAndCode(request.getShopId(), request.getCode())) {
            throw new DuplicateResourceException("Branch code " + request.getCode() + " already exists in this shop");
        }
        Branch branch = new Branch();
        applyRequest(branch, request);
        branch.setShop(shop);
        branch.setStatus(request.getStatus() == null ? BranchStatus.ACTIVE : request.getStatus());
        branch.setIsMainBranch(Boolean.TRUE.equals(request.getIsMainBranch()));
        if (branch.getIsMainBranch()) {
            unsetOtherMainBranches(request.getShopId(), null);
        }
        Branch saved = branchRepository.save(branch);
        auditService.log("CREATE", "Branch", saved.getId().toString(), null, saved.getName() + " (" + saved.getCode() + ")");
        log.info("Branch created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Creates a default active "Main Branch" for a freshly created shop so it is
     * immediately usable for sales, purchases and inventory. Intended for internal
     * use by the shop creation / registration-approval flows, so it performs no
     * caller authorization check. Idempotent: does nothing if the shop already
     * has a main branch.
     */
    @Transactional
    public void createDefaultMainBranch(Shop shop) {
        if (branchRepository.existsByShopIdAndIsMainBranchTrue(shop.getId())) {
            return;
        }
        String code = "MAIN";
        if (branchRepository.existsByShopIdAndCode(shop.getId(), code)) {
            code = "MAIN-" + shop.getId().toString().substring(0, 4).toUpperCase();
        }
        Branch branch = new Branch();
        branch.setShop(shop);
        branch.setName("Main Branch");
        branch.setCode(code);
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setIsMainBranch(true);
        Branch saved = branchRepository.save(branch);
        auditService.log("CREATE", "Branch", saved.getId().toString(), null,
                saved.getName() + " (" + saved.getCode() + ") [auto]");
        log.info("Default main branch created for shop {}", shop.getId());
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(UUID shopId) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        return branchRepository.findByShopIdOrderByNameAsc(shopId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(UUID id) {
        Branch branch = getEntity(id);
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), branch);
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse updateBranch(UUID id, BranchRequest request) {
        log.info("Updating branch: {}", id);
        Branch branch = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), branch.getShop().getId());
        if (!branch.getCode().equalsIgnoreCase(request.getCode())
                && branchRepository.existsByShopIdAndCode(branch.getShop().getId(), request.getCode())) {
            throw new DuplicateResourceException("Branch code " + request.getCode() + " already exists in this shop");
        }
        String oldName = branch.getName() + " (" + branch.getCode() + ")";
        applyRequest(branch, request);
        if (request.getIsMainBranch() != null) {
            boolean wasMain = Boolean.TRUE.equals(branch.getIsMainBranch());
            boolean becomesMain = request.getIsMainBranch();
            if (becomesMain) {
                unsetOtherMainBranches(branch.getShop().getId(), branch.getId());
            }
            branch.setIsMainBranch(becomesMain);
        }
        if (request.getStatus() != null) {
            branch.setStatus(request.getStatus());
        }
        Branch saved = branchRepository.save(branch);
        auditService.log("UPDATE", "Branch", saved.getId().toString(), oldName, saved.getName() + " (" + saved.getCode() + ")");
        log.info("Branch {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        log.info("Deactivating branch: {}", id);
        Branch branch = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), branch.getShop().getId());
        branch.setStatus(BranchStatus.INACTIVE);
        branchRepository.save(branch);
        auditService.log("DELETE", "Branch", branch.getId().toString(), branch.getName(), "INACTIVE");
        log.info("Branch {} deactivated", id);
    }

    private void unsetOtherMainBranches(UUID shopId, UUID exceptId) {
        branchRepository.findByShopIdOrderByNameAsc(shopId).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsMainBranch()))
                .filter(b -> !b.getId().equals(exceptId))
                .forEach(b -> {
                    b.setIsMainBranch(false);
                    branchRepository.save(b);
                });
    }

    private void applyRequest(Branch branch, BranchRequest request) {
        branch.setName(request.getName());
        branch.setCode(request.getCode().toUpperCase());
        branch.setAddress(request.getAddress());
        branch.setContactNumber(request.getContactNumber());
    }

    public Branch getEntity(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .shopId(branch.getShop().getId())
                .shopName(branch.getShop().getName())
                .name(branch.getName())
                .code(branch.getCode())
                .address(branch.getAddress())
                .contactNumber(branch.getContactNumber())
                .isMainBranch(branch.getIsMainBranch())
                .status(branch.getStatus())
                .createdAt(branch.getCreatedAt())
                .build();
    }
}