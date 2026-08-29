package com.smartshop.features.supplier.service;

import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.supplier.dto.SupplierRequest;
import com.smartshop.features.supplier.dto.SupplierResponse;
import com.smartshop.features.supplier.entity.Supplier;
import com.smartshop.features.supplier.repository.SupplierRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.Status;
import com.smartshop.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ShopRepository shopRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        log.info("Creating supplier {} for shop {}", request.getName(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        supplier.setShop(shop);
        supplier.setStatus(request.getStatus() == null ? Status.ACTIVE : request.getStatus());
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier {} created with id: {}", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        log.info("Updating supplier {}", id);
        Supplier supplier = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), supplier.getShop().getId());
        applyRequest(supplier, request);
        if (request.getStatus() != null) {
            supplier.setStatus(request.getStatus());
        }
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deactivating supplier {}", id);
        Supplier supplier = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), supplier.getShop().getId());
        supplier.setStatus(Status.INACTIVE);
        supplierRepository.save(supplier);
        log.info("Supplier {} deactivated", id);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(UUID id) {
        Supplier supplier = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), supplier.getShop().getId());
        return toResponse(supplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> list(UUID shopId, String search, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Supplier> page = supplierRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("phone")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    public Supplier getEntity(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName());
        supplier.setCompanyName(request.getCompanyName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setPanNumber(request.getPanNumber());
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .shopId(supplier.getShop().getId())
                .name(supplier.getName())
                .companyName(supplier.getCompanyName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .panNumber(supplier.getPanNumber())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}