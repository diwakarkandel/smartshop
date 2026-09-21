package com.smartshop.features.sale.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.customer.entity.Customer;
import com.smartshop.features.customer.repository.CustomerRepository;
import com.smartshop.features.inventory.service.InventoryService;
import com.smartshop.features.payment.dto.PaymentRequest;
import com.smartshop.features.payment.service.PaymentService;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.sale.dto.SaleItemRequest;
import com.smartshop.features.sale.dto.SaleItemResponse;
import com.smartshop.features.sale.dto.SaleRequest;
import com.smartshop.features.sale.dto.SaleResponse;
import com.smartshop.features.sale.entity.Sale;
import com.smartshop.features.sale.entity.SaleItem;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.sale.repository.SaleRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.tax.service.TaxService;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.dto.PaymentStatusRequest;
import com.smartshop.shared.enumeration.MovementType;
import com.smartshop.shared.enumeration.PaymentMethod;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.enumeration.Status;
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
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;
    private final TaxService taxService;

    @Transactional
    public SaleResponse create(SaleRequest request) {
        log.info("Creating sale in shop: {}, branch: {} with {} items",
                request.getShopId(), request.getBranchId(), request.getItems() == null ? 0 : request.getItems().size());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), request.getBranchId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        if (!branch.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("Branch does not belong to the given shop");
        }
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
            if (!customer.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Customer does not belong to the given shop");
            }
        }
        User cashier = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        LocalDate billDate = request.getBillDate() == null ? LocalDate.now() : request.getBillDate();
        String invoiceNumber = generateInvoiceNumber(branch.getCode(), billDate);

        Sale sale = new Sale();
        sale.setShop(shop);
        sale.setBranch(branch);
        sale.setCustomer(customer);
        sale.setInvoiceNumber(invoiceNumber);
        sale.setBillDate(billDate);
        sale.setRemarks(request.getRemarks());
        sale.setCashier(cashier);
        sale.setPaymentStatus(PaymentStatus.UNPAID);

        BigDecimal headerDiscount = request.getDiscountAmount() == null
                ? BigDecimal.ZERO : request.getDiscountAmount().setScale(2, RoundingMode.HALF_UP);
        sale.setDiscountAmount(headerDiscount);

        List<SaleItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vatAmountTotal = BigDecimal.ZERO;
        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));
            if (!product.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Product " + product.getSku() + " does not belong to the given shop");
            }
            if (product.getStatus() != Status.ACTIVE) {
                throw new BadRequestException("Product " + product.getSku() + " is not active");
            }
            BigDecimal qty = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice() == null
                    ? product.getSellingPrice() : itemReq.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineGross = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemDiscount = itemReq.getDiscountAmount() == null
                    ? BigDecimal.ZERO : itemReq.getDiscountAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxable = lineGross.subtract(itemDiscount);
            // Prefer dynamic Tax entity over legacy vatRate
            BigDecimal vatRate;
            if (product.getTax() != null) {
                vatRate = taxService.getActiveRate(product.getTax().getId(), billDate);
            } else if (itemReq.getVatRate() != null) {
                vatRate = itemReq.getVatRate();
            } else {
                vatRate = product.getVatRate();
            }
            BigDecimal vatAmount = taxable.multiply(vatRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = taxable.add(vatAmount);

            BigDecimal unitCostAtSale = inventoryService.getAverageCost(branch.getId(), product.getId());
            BigDecimal cogs = qty.multiply(unitCostAtSale).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineProfit = taxable.subtract(cogs).setScale(2, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            item.setUnitCostAtSale(unitCostAtSale);
            item.setLineProfit(lineProfit);
            item.setDiscountAmount(itemDiscount);
            item.setVatRate(vatRate);
            item.setVatAmount(vatAmount);
            item.setLineTotal(lineTotal);
            items.add(item);

            subtotal = subtotal.add(lineGross);
            vatAmountTotal = vatAmountTotal.add(vatAmount);
        }

        sale.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        BigDecimal taxableAmount = subtotal.subtract(headerDiscount).setScale(2, RoundingMode.HALF_UP);
        sale.setTaxableAmount(taxableAmount);
        sale.setVatAmount(vatAmountTotal.setScale(2, RoundingMode.HALF_UP));
        sale.setTotalAmount(taxableAmount.add(vatAmountTotal).setScale(2, RoundingMode.HALF_UP));
        saleRepository.save(sale);

        for (SaleItem item : items) {
            item.setSale(sale);
            saleItemRepository.save(item);
            inventoryService.adjustStock(branch.getId(), item.getProduct().getId(), item.getQuantity(),
                    MovementType.SALE_OUT, "SALE", sale.getId(), null, null);
        }

        BigDecimal cashTendered = request.getCashTendered() == null ? null : request.getCashTendered().setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = BigDecimal.ZERO;
        PaymentMethod method = request.getPaymentMethod();
        boolean wantsPaid = request.getPaymentStatus() == PaymentStatus.PAID
                || request.getPaymentStatus() == PaymentStatus.PARTIAL
                || method != null || cashTendered != null;
        if (wantsPaid) {
            if (method == null || method == PaymentMethod.CASH) {
                method = PaymentMethod.CASH;
                BigDecimal tendered = cashTendered != null ? cashTendered : sale.getTotalAmount();
                if (tendered.compareTo(sale.getTotalAmount()) < 0) {
                    throw new BadRequestException("Cash tendered is less than the total amount");
                }
                change = tendered.subtract(sale.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
                sale.setPaymentMethod(method);
                paymentService.recordPayment(new PaymentRequest(sale.getId(), sale.getTotalAmount(),
                        PaymentMethod.CASH, null));
            } else {
                sale.setPaymentMethod(method);
                sale.setQrReference(request.getQrReference());
                paymentService.recordPayment(new PaymentRequest(sale.getId(), sale.getTotalAmount(),
                        method, request.getQrReference()));
            }
        } else {
            sale.setPaymentStatus(request.getPaymentStatus() == null ? PaymentStatus.UNPAID : request.getPaymentStatus());
            saleRepository.save(sale);
        }

        if (customer != null) {
            customer.setLoyaltyPoints(customer.getLoyaltyPoints()
                    .add(new BigDecimal(sale.getTotalAmount().intValue())));
            customerRepository.save(customer);
        }

        auditService.log("CREATE", "Sale", sale.getId().toString(), null,
                "Invoice: " + sale.getInvoiceNumber() + ", Total: " + sale.getTotalAmount());
        log.info("Sale created successfully with invoice: {} and total: {}", sale.getInvoiceNumber(), sale.getTotalAmount());

        SaleResponse response = toResponse(sale);
        response.setCashTendered(cashTendered);
        response.setChangeAmount(change);
        return response;
    }

    @Transactional(readOnly = true)
    public SaleResponse get(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
        branchScopeGuard.requireShopAccessOrNotFound(SecurityUtils.currentUserId(), sale.getShop().getId());
        return toResponse(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> list(UUID shopId, String search, UUID branchId, UUID customerId, PaymentStatus paymentStatus,
                                   LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Sale> page = saleRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("billDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("billDate"), dateTo));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase() + "%";
                var customerJoin = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNumber")), term),
                        cb.like(cb.lower(customerJoin.get("name")), term),
                        cb.like(cb.lower(customerJoin.get("phone")), term)));
            }
            query.orderBy(cb.desc(root.get("billDate")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public SaleResponse updatePaymentStatus(UUID id, PaymentStatusRequest request) {
        log.info("Updating payment status for sale: {} to {}", id, request.getPaymentStatus());
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), sale.getShop().getId());
        PaymentStatus oldStatus = sale.getPaymentStatus();
        sale.setPaymentStatus(request.getPaymentStatus());
        saleRepository.save(sale);
        auditService.log("PAYMENT_STATUS_CHANGE", "Sale", sale.getId().toString(),
                oldStatus == null ? null : oldStatus.name(), request.getPaymentStatus().name());
        return toResponse(sale);
    }

    private String generateInvoiceNumber(String branchCode, LocalDate date) {
        String datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = saleRepository.countByBranchCodeAndDate(branchCode, date);
        return String.format("INV-%s-%s-%03d", branchCode, datePart, count + 1);
    }

    private SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> itemResponses = saleItemRepository.findBySaleId(sale.getId())
                .stream().map(item -> SaleItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .unitCostAtSale(item.getUnitCostAtSale())
                        .lineProfit(item.getLineProfit())
                        .discountAmount(item.getDiscountAmount())
                        .vatRate(item.getVatRate())
                        .vatAmount(item.getVatAmount())
                        .taxableAmount(item.getLineTotal().subtract(item.getVatAmount()))
                        .lineTotal(item.getLineTotal())
                        .build()).toList();
        return SaleResponse.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .billDate(sale.getBillDate())
                .shopId(sale.getShop().getId())
                .branchId(sale.getBranch().getId())
                .branchName(sale.getBranch().getName())
                .branchCode(sale.getBranch().getCode())
                .customerId(sale.getCustomer() == null ? null : sale.getCustomer().getId())
                .customerName(sale.getCustomer() == null ? null : sale.getCustomer().getName())
                .customerPhone(sale.getCustomer() == null ? null : sale.getCustomer().getPhone())
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxableAmount(sale.getTaxableAmount())
                .vatAmount(sale.getVatAmount())
                .totalAmount(sale.getTotalAmount())
                .paymentStatus(sale.getPaymentStatus())
                .paymentMethod(sale.getPaymentMethod())
                .qrReference(sale.getQrReference())
                .cashierId(sale.getCashier().getId())
                .cashierName(sale.getCashier().getFullName())
                .remarks(sale.getRemarks())
                .createdAt(sale.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}