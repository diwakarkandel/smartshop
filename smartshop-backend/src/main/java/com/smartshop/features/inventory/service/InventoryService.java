package com.smartshop.features.inventory.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.dto.InventoryResponse;
import com.smartshop.features.inventory.dto.StockMovementResponse;
import com.smartshop.features.inventory.entity.Inventory;
import com.smartshop.features.inventory.entity.StockMovement;
import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.inventory.repository.StockMovementRepository;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.MovementType;
import com.smartshop.shared.exception.BusinessException;
import com.smartshop.shared.exception.InsufficientStockException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public void adjustStock(UUID branchId, UUID productId, BigDecimal quantity, MovementType type,
                            String referenceType, UUID referenceId, String note, BigDecimal unitCost) {
        log.info("Adjusting stock for branch: {}, product: {}, qty: {}, type: {}", branchId, productId, quantity, type);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Movement quantity must be positive");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Inventory inventory = inventoryRepository.findByBranchIdAndProductIdForUpdate(branchId, productId)
                .orElseGet(() -> {
                    if (type.name().endsWith("_OUT")) {
                        throw new InsufficientStockException(product.getName(), quantity, BigDecimal.ZERO);
                    }
                    Inventory created = new Inventory();
                    created.setBranch(branch);
                    created.setProduct(product);
                    created.setQuantityAvailable(BigDecimal.ZERO);
                    created.setQuantityReserved(BigDecimal.ZERO);
                    created.setAverageCost(BigDecimal.ZERO);
                    return inventoryRepository.save(created);
                });

        boolean isIn = type.name().endsWith("_IN");
        if (isIn) {
            BigDecimal newQty = inventory.getQuantityAvailable().add(quantity);
            inventory.setQuantityAvailable(newQty);
            if ((type == MovementType.PURCHASE_IN || type == MovementType.TRANSFER_IN) && unitCost != null) {
                BigDecimal oldQty = inventory.getQuantityAvailable().subtract(quantity);
                BigDecimal oldCost = inventory.getAverageCost();
                BigDecimal totalOldValue = oldQty.multiply(oldCost);
                BigDecimal totalNewValue = quantity.multiply(unitCost);
                BigDecimal avgCost = totalOldValue.add(totalNewValue)
                        .divide(oldQty.add(quantity), 4, RoundingMode.HALF_UP)
                        .setScale(2, RoundingMode.HALF_UP);
                inventory.setAverageCost(avgCost);
            }
        } else {
            if (inventory.getQuantityAvailable().compareTo(quantity) < 0) {
                throw new InsufficientStockException(product.getName(), quantity, inventory.getQuantityAvailable());
            }
            inventory.setQuantityAvailable(inventory.getQuantityAvailable().subtract(quantity));
        }
        inventory.setLastStockUpdate(LocalDateTime.now());
        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setBranch(branch);
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movement.setCreatedBy(currentUserOrNull());
        stockMovementRepository.save(movement);
        log.info("Stock adjusted successfully. New available qty: {}", inventory.getQuantityAvailable());
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> listForBranch(UUID branchId, String search, Boolean lowStockOnly, Pageable pageable) {
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), branchId);
        Page<Inventory> page = inventoryRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("product").get("name")), like),
                        cb.like(cb.lower(root.get("product").get("sku")), like),
                        cb.like(cb.lower(root.get("product").get("barcode")), like)));
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantityAvailable"), root.get("product").get("reorderLevel")));
            }
            query.orderBy(cb.desc(root.get("quantityAvailable")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toInventoryResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProduct(UUID branchId, UUID productId) {
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), branchId);
        Inventory inventory = inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new BusinessException("No inventory record for this product in the branch"));
        return toInventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageCost(UUID branchId, UUID productId) {
        return inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .map(Inventory::getAverageCost)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID shopId, UUID branchId, UUID productId,
                                                     MovementType movementType, LocalDate dateFrom, LocalDate dateTo,
                                                     Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<StockMovement> page = stockMovementRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branch").get("shop").get("id"), shopId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(23, 59, 59)));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toMovementResponse);
    }

    private InventoryResponse toInventoryResponse(Inventory inventory) {
        BigDecimal qty = inventory.getQuantityAvailable();
        BigDecimal reorder = inventory.getProduct().getReorderLevel();
        String stockStatus;
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (qty.compareTo(reorder) <= 0) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }
        return InventoryResponse.builder()
                .id(inventory.getId())
                .branchId(inventory.getBranch().getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .sku(inventory.getProduct().getSku())
                .barcode(inventory.getProduct().getBarcode())
                .categoryName(inventory.getProduct().getCategory() == null
                        ? null : inventory.getProduct().getCategory().getName())
                .unit(inventory.getProduct().getUnit())
                .quantityAvailable(qty)
                .quantityReserved(inventory.getQuantityReserved())
                .averageCost(inventory.getAverageCost())
                .reorderLevel(reorder)
                .stockStatus(stockStatus)
                .lastStockUpdate(inventory.getLastStockUpdate())
                .build();
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .branchId(movement.getBranch().getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .sku(movement.getProduct().getSku())
                .movementType(movement.getMovementType().name())
                .quantity(movement.getQuantity())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .note(movement.getNote())
                .createdByName(movement.getCreatedBy() == null ? null : movement.getCreatedBy().getFullName())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private User currentUserOrNull() {
        try {
            return userRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}