package com.smartshop.features.saleReturn.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.features.sale.entity.Sale;
import com.smartshop.features.sale.entity.SaleItem;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.sale.repository.SaleRepository;
import com.smartshop.features.saleReturn.dto.SaleReturnItemRequest;
import com.smartshop.features.saleReturn.dto.SaleReturnItemResponse;
import com.smartshop.features.saleReturn.dto.SaleReturnRequest;
import com.smartshop.features.saleReturn.dto.SaleReturnResponse;
import com.smartshop.features.saleReturn.entity.SaleReturn;
import com.smartshop.features.saleReturn.entity.SaleReturnItem;
import com.smartshop.features.saleReturn.repository.SaleReturnItemRepository;
import com.smartshop.features.saleReturn.repository.SaleReturnRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
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
public class SaleReturnService {

    private final SaleReturnRepository saleReturnRepository;
    private final SaleReturnItemRepository saleReturnItemRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;

    @Transactional
    public SaleReturnResponse create(SaleReturnRequest request) {
        log.info("Processing sale return for sale: {} in branch: {}", request.getSaleId(), request.getBranchId());
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", request.getSaleId()));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), sale.getShop().getId());
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), request.getBranchId());
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        if (!branch.getShop().getId().equals(sale.getShop().getId())) {
            throw new BadRequestException("Branch does not belong to the sale's shop");
        }
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        LocalDate returnDate = request.getReturnDate() == null ? LocalDate.now() : request.getReturnDate();
        String returnNumber = generateReturnNumber(branch.getCode(), returnDate);

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setSale(sale);
        saleReturn.setBranch(branch);
        saleReturn.setReturnNumber(returnNumber);
        saleReturn.setReturnDate(returnDate);
        saleReturn.setReason(request.getReason());
        saleReturn.setCreatedBy(createdBy);
        saleReturnRepository.save(saleReturn);

        BigDecimal refundTotal = BigDecimal.ZERO;
        List<SaleReturnItem> items = new ArrayList<>();
        for (SaleReturnItemRequest itemReq : request.getItems()) {
            SaleItem saleItem = saleItemRepository.findById(itemReq.getSaleItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("SaleItem", itemReq.getSaleItemId()));
            if (!saleItem.getSale().getId().equals(sale.getId())) {
                throw new BadRequestException("Sale item does not belong to the given sale");
            }
            if (!saleItem.getProduct().getId().equals(itemReq.getProductId())) {
                throw new BadRequestException("Product does not match the sale item");
            }
            BigDecimal alreadyReturned = saleReturnItemRepository.sumReturnedQuantityBySaleItemId(saleItem.getId());
            BigDecimal returnable = saleItem.getQuantity().subtract(alreadyReturned);
            if (itemReq.getQuantity().compareTo(returnable) > 0) {
                throw new BadRequestException("Return quantity for " + saleItem.getProduct().getName()
                        + " exceeds the sold quantity (returnable: " + returnable.toPlainString() + ")");
            }
            BigDecimal lineTotal = itemReq.getQuantity().multiply(saleItem.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            SaleReturnItem item = new SaleReturnItem();
            item.setSaleReturn(saleReturn);
            item.setSaleItem(saleItem);
            item.setProduct(saleItem.getProduct());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(saleItem.getUnitPrice());
            item.setLineTotal(lineTotal);
            items.add(item);

            refundTotal = refundTotal.add(lineTotal);
            inventoryService.adjustStock(branch.getId(), saleItem.getProduct().getId(), itemReq.getQuantity(),
                    MovementType.SALE_RETURN_IN, "SALE_RETURN", saleReturn.getId(), "Return " + returnNumber, null);
        }

        saleReturn.setRefundAmount(refundTotal.setScale(2, RoundingMode.HALF_UP));
        SaleReturn saved = saleReturnRepository.save(saleReturn);
        for (SaleReturnItem item : items) {
            item.setSaleReturn(saved);
            saleReturnItemRepository.save(item);
        }
        auditService.log("CREATE", "SaleReturn", saved.getId().toString(), null,
                saved.getReturnNumber() + " refund=" + saved.getRefundAmount());
        log.info("Sale return {} created with refund amount {}", saved.getReturnNumber(), saved.getRefundAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleReturnResponse get(UUID id) {
        SaleReturn saleReturn = saleReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SaleReturn", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), saleReturn.getSale().getShop().getId());
        return toResponse(saleReturn);
    }

    @Transactional(readOnly = true)
    public Page<SaleReturnResponse> list(UUID shopId, UUID branchId, LocalDate dateFrom, LocalDate dateTo,
                                         Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<SaleReturn> page = saleReturnRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sale").get("shop").get("id"), shopId));
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
        long count = saleReturnRepository.countByBranchCodeAndDate(branchCode, date);
        return String.format("RT-%s-%s-%03d", branchCode, datePart, count + 1);
    }

    private SaleReturnResponse toResponse(SaleReturn saleReturn) {
        List<SaleReturnItemResponse> itemResponses = saleReturnItemRepository.findBySaleReturnId(saleReturn.getId())
                .stream().map(item -> SaleReturnItemResponse.builder()
                        .id(item.getId())
                        .saleItemId(item.getSaleItem().getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build()).toList();
        return SaleReturnResponse.builder()
                .id(saleReturn.getId())
                .returnNumber(saleReturn.getReturnNumber())
                .returnDate(saleReturn.getReturnDate())
                .saleId(saleReturn.getSale().getId())
                .invoiceNumber(saleReturn.getSale().getInvoiceNumber())
                .branchId(saleReturn.getBranch().getId())
                .branchName(saleReturn.getBranch().getName())
                .reason(saleReturn.getReason())
                .refundAmount(saleReturn.getRefundAmount())
                .createdById(saleReturn.getCreatedBy().getId())
                .createdByName(saleReturn.getCreatedBy().getFullName())
                .createdAt(saleReturn.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}