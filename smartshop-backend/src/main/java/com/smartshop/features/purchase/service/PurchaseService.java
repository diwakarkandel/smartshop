package com.smartshop.features.purchase.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.purchase.dto.PurchaseItemRequest;
import com.smartshop.features.purchase.dto.PurchaseItemResponse;
import com.smartshop.features.purchase.dto.PurchaseRequest;
import com.smartshop.features.purchase.dto.PurchaseResponse;
import com.smartshop.features.purchase.entity.Purchase;
import com.smartshop.features.purchase.entity.PurchaseItem;
import com.smartshop.features.purchase.repository.PurchaseItemRepository;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.supplier.entity.Supplier;
import com.smartshop.features.supplier.repository.SupplierRepository;
import com.smartshop.features.tax.service.TaxService;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.MovementType;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.enumeration.Status;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import com.smartshop.shared.dto.PaymentStatusRequest;
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
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;
    private final TaxService taxService;

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        log.info("Creating purchase for shop: {}, branch: {}, supplier: {}",
                request.getShopId(), request.getBranchId(), request.getSupplierId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), request.getBranchId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        if (!branch.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("Branch does not belong to the given shop");
        }
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
        if (!supplier.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("Supplier does not belong to the given shop");
        }
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        LocalDate purchaseDate = request.getPurchaseDate() == null ? LocalDate.now() : request.getPurchaseDate();
        String purchaseNumber = generatePurchaseNumber(branch.getCode(), purchaseDate);

        Purchase purchase = new Purchase();
        purchase.setShop(shop);
        purchase.setBranch(branch);
        purchase.setSupplier(supplier);
        purchase.setPurchaseNumber(purchaseNumber);
        purchase.setPurchaseDate(purchaseDate);
        purchase.setNotes(request.getNotes());
        purchase.setCreatedBy(createdBy);
        purchase.setPaymentStatus(request.getPaymentStatus() == null ? PaymentStatus.UNPAID : request.getPaymentStatus());

        BigDecimal headerDiscount = request.getDiscountAmount() == null
                ? BigDecimal.ZERO : request.getDiscountAmount().setScale(2, RoundingMode.HALF_UP);
        purchase.setDiscountAmount(headerDiscount);

        List<PurchaseItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vatAmountTotal = BigDecimal.ZERO;
        for (PurchaseItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));
            if (!product.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Product " + product.getSku() + " does not belong to the given shop");
            }
            if (product.getStatus() != Status.ACTIVE) {
                throw new BadRequestException("Product " + product.getSku() + " is not active");
            }
            BigDecimal qty = itemReq.getQuantity();
            BigDecimal unitCost = itemReq.getUnitCost().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineGross = qty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemDiscount = itemReq.getDiscountAmount() == null
                    ? BigDecimal.ZERO : itemReq.getDiscountAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxable = lineGross.subtract(itemDiscount);
            // Prefer dynamic Tax entity over legacy vatRate
            BigDecimal vatRate;
            if (product.getTax() != null) {
                vatRate = taxService.getActiveRate(product.getTax().getId(), purchase.getPurchaseDate());
            } else if (itemReq.getVatRate() != null) {
                vatRate = itemReq.getVatRate();
            } else {
                vatRate = product.getVatRate();
            }
            BigDecimal vatAmount = taxable.multiply(vatRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = taxable.add(vatAmount);

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setUnitCost(unitCost);
            item.setDiscountAmount(itemDiscount);
            item.setVatRate(vatRate);
            item.setVatAmount(vatAmount);
            item.setLineTotal(lineTotal);
            items.add(item);

            subtotal = subtotal.add(lineGross);
            vatAmountTotal = vatAmountTotal.add(vatAmount);
        }

        purchase.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        BigDecimal taxableAmount = subtotal.subtract(headerDiscount).setScale(2, RoundingMode.HALF_UP);
        purchase.setTaxableAmount(taxableAmount);
        purchase.setVatAmount(vatAmountTotal.setScale(2, RoundingMode.HALF_UP));
        purchase.setTotalAmount(taxableAmount.add(vatAmountTotal).setScale(2, RoundingMode.HALF_UP));
        Purchase saved = purchaseRepository.save(purchase);

        for (PurchaseItem item : items) {
            item.setPurchase(saved);
            purchaseItemRepository.save(item);
            inventoryService.adjustStock(branch.getId(), item.getProduct().getId(), item.getQuantity(),
                    MovementType.PURCHASE_IN, "PURCHASE", saved.getId(), null, item.getUnitCost());
        }

        auditService.log("CREATE", "Purchase", saved.getId().toString(), null,
                "PO: " + saved.getPurchaseNumber() + ", Total: " + saved.getTotalAmount());
        log.info("Purchase created successfully with number: {} and total: {}", saved.getPurchaseNumber(), saved.getTotalAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(UUID id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), purchase.getShop().getId());
        return toResponse(purchase);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> list(UUID shopId, String search, UUID branchId, PaymentStatus paymentStatus,
                                       LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Purchase> page = purchaseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), dateTo));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("purchaseNumber")), "%" + search.toLowerCase() + "%"));
            }
            query.orderBy(cb.desc(root.get("purchaseDate")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public PurchaseResponse updatePaymentStatus(UUID id, PaymentStatusRequest request) {
        log.info("Updating payment status for purchase: {} to {}", id, request.getPaymentStatus());
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), purchase.getShop().getId());
        PaymentStatus oldStatus = purchase.getPaymentStatus();
        purchase.setPaymentStatus(request.getPaymentStatus());
        purchaseRepository.save(purchase);
        auditService.log("UPDATE", "Purchase", purchase.getId().toString(),
                oldStatus == null ? null : oldStatus.name(), request.getPaymentStatus().name());
        return toResponse(purchase);
    }

    private String generatePurchaseNumber(String branchCode, LocalDate date) {
        String datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = purchaseRepository.countByBranchCodeAndDate(branchCode, date);
        return String.format("PO-%s-%s-%03d", branchCode, datePart, count + 1);
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        List<PurchaseItemResponse> itemResponses = purchaseItemRepository.findByPurchaseId(purchase.getId())
                .stream().map(item -> PurchaseItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .discountAmount(item.getDiscountAmount())
                        .vatRate(item.getVatRate())
                        .vatAmount(item.getVatAmount())
                        .taxableAmount(item.getLineTotal().subtract(item.getVatAmount()))
                        .lineTotal(item.getLineTotal())
                        .build()).toList();
        return PurchaseResponse.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .purchaseDate(purchase.getPurchaseDate())
                .shopId(purchase.getShop().getId())
                .branchId(purchase.getBranch().getId())
                .branchName(purchase.getBranch().getName())
                .branchCode(purchase.getBranch().getCode())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getName())
                .subtotal(purchase.getSubtotal())
                .discountAmount(purchase.getDiscountAmount())
                .taxableAmount(purchase.getTaxableAmount())
                .vatAmount(purchase.getVatAmount())
                .totalAmount(purchase.getTotalAmount())
                .paymentStatus(purchase.getPaymentStatus())
                .notes(purchase.getNotes())
                .createdById(purchase.getCreatedBy().getId())
                .createdByName(purchase.getCreatedBy().getFullName())
                .createdAt(purchase.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}