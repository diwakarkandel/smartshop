package com.smartshop.features.stockTransfer.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.stockTransfer.dto.StockTransferItemRequest;
import com.smartshop.features.stockTransfer.dto.StockTransferItemResponse;
import com.smartshop.features.stockTransfer.dto.StockTransferRequest;
import com.smartshop.features.stockTransfer.dto.StockTransferResponse;
import com.smartshop.features.stockTransfer.entity.StockTransfer;
import com.smartshop.features.stockTransfer.entity.StockTransferItem;
import com.smartshop.features.stockTransfer.repository.StockTransferItemRepository;
import com.smartshop.features.stockTransfer.repository.StockTransferRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.MovementType;
import com.smartshop.shared.enumeration.TransferStatus;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferItemRepository stockTransferItemRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    @Transactional
    public StockTransferResponse create(StockTransferRequest request) {
        log.info("Creating stock transfer from branch {} to branch {} in shop {}",
                request.getFromBranchId(), request.getToBranchId(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Branch fromBranch = validateBranch(request.getFromBranchId(), shop);
        Branch toBranch = validateBranch(request.getToBranchId(), shop);
        if (fromBranch.getId().equals(toBranch.getId())) {
            throw new BadRequestException("From and to branches must be different");
        }
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        StockTransfer transfer = new StockTransfer();
        transfer.setShop(shop);
        transfer.setFromBranch(fromBranch);
        transfer.setToBranch(toBranch);
        transfer.setTransferNumber(generateTransferNumber(fromBranch.getCode(), LocalDate.now()));
        transfer.setNote(request.getNote());
        transfer.setCreatedBy(createdBy);
        transfer.setStatus(TransferStatus.PENDING);
        StockTransfer saved = stockTransferRepository.save(transfer);

        for (StockTransferItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));
            if (!product.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Product " + product.getSku() + " does not belong to the given shop");
            }
            StockTransferItem item = new StockTransferItem();
            item.setStockTransfer(saved);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            stockTransferItemRepository.save(item);
        }

        auditService.log("CREATE", "StockTransfer", saved.getId().toString(), null,
                "Transfer: " + saved.getTransferNumber() + " (PENDING)");
        log.info("Stock transfer {} created with status PENDING", saved.getTransferNumber());
        return toResponse(saved);
    }

    @Transactional
    public StockTransferResponse approve(UUID id) {
        log.info("Approving stock transfer: {}", id);
        StockTransfer transfer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), transfer.getShop().getId());
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Only pending transfers can be approved");
        }
        List<StockTransferItem> items = stockTransferItemRepository.findByStockTransferId(transfer.getId());
        for (StockTransferItem item : items) {
            BigDecimal avgCost = inventoryService.getAverageCost(
                    transfer.getFromBranch().getId(), item.getProduct().getId());
            inventoryService.adjustStock(transfer.getFromBranch().getId(), item.getProduct().getId(),
                    item.getQuantity(), MovementType.TRANSFER_OUT, "TRANSFER", transfer.getId(),
                    transfer.getNote(), null);
            inventoryService.adjustStock(transfer.getToBranch().getId(), item.getProduct().getId(),
                    item.getQuantity(), MovementType.TRANSFER_IN, "TRANSFER", transfer.getId(),
                    transfer.getNote(), avgCost);
        }
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(currentUser());
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditService.log("APPROVE", "StockTransfer", saved.getId().toString(), "PENDING", "APPROVED");
        log.info("Stock transfer {} approved successfully", saved.getTransferNumber());
        return toResponse(saved);
    }

    @Transactional
    public StockTransferResponse reject(UUID id) {
        log.info("Rejecting stock transfer: {}", id);
        StockTransfer transfer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), transfer.getShop().getId());
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Only pending transfers can be rejected");
        }
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setApprovedBy(currentUser());
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditService.log("REJECT", "StockTransfer", saved.getId().toString(), "PENDING", "REJECTED");
        log.info("Stock transfer {} rejected", saved.getTransferNumber());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse get(UUID id) {
        StockTransfer transfer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), transfer.getShop().getId());
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponse> list(UUID shopId, TransferStatus status, UUID branchId, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<StockTransfer> page = stockTransferRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (branchId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("fromBranch").get("id"), branchId),
                        cb.equal(root.get("toBranch").get("id"), branchId)));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    private Branch validateBranch(UUID branchId, Shop shop) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        if (!branch.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("Branch does not belong to the given shop");
        }
        return branch;
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));
    }

    private String generateTransferNumber(String branchCode, LocalDate date) {
        String datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = stockTransferRepository.countByFromBranchCodeAndDate(branchCode, date);
        return String.format("TR-%s-%s-%03d", branchCode, datePart, count + 1);
    }

    public StockTransfer getEntity(UUID id) {
        return stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", id));
    }

    private StockTransferResponse toResponse(StockTransfer transfer) {
        List<StockTransferItemResponse> itemResponses = stockTransferItemRepository
                .findByStockTransferId(transfer.getId())
                .stream().map(item -> StockTransferItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .build()).toList();
        return StockTransferResponse.builder()
                .id(transfer.getId())
                .transferNumber(transfer.getTransferNumber())
                .shopId(transfer.getShop().getId())
                .fromBranchId(transfer.getFromBranch().getId())
                .fromBranchName(transfer.getFromBranch().getName())
                .toBranchId(transfer.getToBranch().getId())
                .toBranchName(transfer.getToBranch().getName())
                .status(transfer.getStatus())
                .note(transfer.getNote())
                .createdById(transfer.getCreatedBy().getId())
                .createdByName(transfer.getCreatedBy().getFullName())
                .approvedById(transfer.getApprovedBy() == null ? null : transfer.getApprovedBy().getId())
                .approvedByName(transfer.getApprovedBy() == null ? null : transfer.getApprovedBy().getFullName())
                .createdAt(transfer.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}