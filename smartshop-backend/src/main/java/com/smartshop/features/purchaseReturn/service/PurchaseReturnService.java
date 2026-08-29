package com.smartshop.features.purchaseReturn.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.features.purchase.entity.Purchase;
import com.smartshop.features.purchase.entity.PurchaseItem;
import com.smartshop.features.purchase.repository.PurchaseItemRepository;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.purchaseReturn.dto.PurchaseReturnItemRequest;
import com.smartshop.features.purchaseReturn.dto.PurchaseReturnItemResponse;
import com.smartshop.features.purchaseReturn.dto.PurchaseReturnRequest;
import com.smartshop.features.purchaseReturn.dto.PurchaseReturnResponse;
import com.smartshop.features.purchaseReturn.entity.PurchaseReturn;
import com.smartshop.features.purchaseReturn.entity.PurchaseReturnItem;
import com.smartshop.features.purchaseReturn.repository.PurchaseReturnItemRepository;
import com.smartshop.features.purchaseReturn.repository.PurchaseReturnRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.MovementType;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;
    private final BranchRepository branchRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public PurchaseReturnResponse create(PurchaseReturnRequest request) {
        log.info("Processing purchase return for purchase: {} in branch: {}", request.getPurchaseId(), request.getBranchId());
        Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", request.getPurchaseId()));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), purchase.getShop().getId());
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), request.getBranchId());
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        if (!branch.getShop().getId().equals(purchase.getShop().getId())) {
            throw new BadRequestException("Branch does not belong to the purchase's shop");
        }
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        LocalDate returnDate = request.getReturnDate() == null ? LocalDate.now() : request.getReturnDate();
        String returnNumber = generateReturnNumber(branch.getCode(), returnDate);

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setPurchase(purchase);
        purchaseReturn.setBranch(branch);
        purchaseReturn.setReturnNumber(returnNumber);
        purchaseReturn.setReturnDate(returnDate);
        purchaseReturn.setReason(request.getReason());
        purchaseReturn.setCreatedBy(createdBy);
        purchaseReturnRepository.save(purchaseReturn);

        BigDecimal refundTotal = BigDecimal.ZERO;
        List<PurchaseReturnItem> items = new ArrayList<>();
        for (PurchaseReturnItemRequest itemReq : request.getItems()) {
            PurchaseItem purchaseItem = purchaseItemRepository.findById(itemReq.getPurchaseItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseItem", itemReq.getPurchaseItemId()));
            if (!purchaseItem.getPurchase().getId().equals(purchase.getId())) {
                throw new BadRequestException("Purchase item does not belong to the given purchase");
            }
            if (!purchaseItem.getProduct().getId().equals(itemReq.getProductId())) {
                throw new BadRequestException("Product does not match the purchase item");
            }
            BigDecimal alreadyReturned = purchaseReturnItemRepository.sumReturnedQuantityByPurchaseItemId(purchaseItem.getId());
            BigDecimal returnable = purchaseItem.getQuantity().subtract(alreadyReturned);
            if (itemReq.getQuantity().compareTo(returnable) > 0) {
                throw new BadRequestException("Return quantity for " + purchaseItem.getProduct().getName()
                        + " exceeds the purchased quantity (returnable: " + returnable.toPlainString() + ")");
            }
            BigDecimal lineTotal = itemReq.getQuantity().multiply(purchaseItem.getUnitCost())
                    .setScale(2, RoundingMode.HALF_UP);
            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setPurchaseReturn(purchaseReturn);
            item.setPurchaseItem(purchaseItem);
            item.setProduct(purchaseItem.getProduct());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitCost(purchaseItem.getUnitCost());
            item.setLineTotal(lineTotal);
            items.add(item);

            refundTotal = refundTotal.add(lineTotal);
            inventoryService.adjustStock(branch.getId(), purchaseItem.getProduct().getId(), itemReq.getQuantity(),
                    MovementType.PURCHASE_RETURN_OUT, "PURCHASE_RETURN", purchaseReturn.getId(),
                    "Return " + returnNumber, null);
        }

        purchaseReturn.setRefundAmount(refundTotal.setScale(2, RoundingMode.HALF_UP));
        PurchaseReturn saved = purchaseReturnRepository.save(purchaseReturn);
        for (PurchaseReturnItem item : items) {
            item.setPurchaseReturn(saved);
            purchaseReturnItemRepository.save(item);
        }
        log.info("Purchase return {} created with refund amount {}", saved.getReturnNumber(), saved.getRefundAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PurchaseReturnResponse get(UUID id) {
        PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseReturn", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(),
                purchaseReturn.getPurchase().getShop().getId());
        return toResponse(purchaseReturn);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseReturnResponse> list(UUID shopId, UUID branchId, LocalDate dateFrom, LocalDate dateTo,
                                             Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<PurchaseReturn> page = purchaseReturnRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("purchase").get("shop").get("id"), shopId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("returnDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("returnDate"), dateTo));
            }
            query.orderBy(cb.desc(root.get("returnDate")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    private String generateReturnNumber(String branchCode, LocalDate date) {
        String datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = purchaseReturnRepository.countByBranchCodeAndDate(branchCode, date);
        return String.format("PR-%s-%s-%03d", branchCode, datePart, count + 1);
    }

    private PurchaseReturnResponse toResponse(PurchaseReturn purchaseReturn) {
        List<PurchaseReturnItemResponse> itemResponses = purchaseReturnItemRepository
                .findByPurchaseReturnId(purchaseReturn.getId())
                .stream().map(item -> PurchaseReturnItemResponse.builder()
                        .id(item.getId())
                        .purchaseItemId(item.getPurchaseItem().getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .lineTotal(item.getLineTotal())
                        .build()).toList();
        return PurchaseReturnResponse.builder()
                .id(purchaseReturn.getId())
                .returnNumber(purchaseReturn.getReturnNumber())
                .returnDate(purchaseReturn.getReturnDate())
                .purchaseId(purchaseReturn.getPurchase().getId())
                .purchaseNumber(purchaseReturn.getPurchase().getPurchaseNumber())
                .branchId(purchaseReturn.getBranch().getId())
                .branchName(purchaseReturn.getBranch().getName())
                .reason(purchaseReturn.getReason())
                .refundAmount(purchaseReturn.getRefundAmount())
                .createdById(purchaseReturn.getCreatedBy().getId())
                .createdByName(purchaseReturn.getCreatedBy().getFullName())
                .createdAt(purchaseReturn.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}