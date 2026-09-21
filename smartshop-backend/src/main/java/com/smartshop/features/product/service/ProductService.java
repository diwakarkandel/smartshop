package com.smartshop.features.product.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.category.entity.Category;
import com.smartshop.features.category.repository.CategoryRepository;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.inventory.repository.InventoryRepository;
import com.smartshop.features.product.dto.ProductRequest;
import com.smartshop.features.product.dto.ProductResponse;
import com.smartshop.features.product.dto.ProductSearchResponse;
import com.smartshop.features.product.entity.Product;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.pricing.service.PricingService;
import com.smartshop.features.purchase.repository.PurchaseItemRepository;
import com.smartshop.features.sale.repository.SaleItemRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.settings.service.SettingsService;
import com.smartshop.features.tax.entity.Tax;
import com.smartshop.features.tax.repository.TaxRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.Status;
import com.smartshop.shared.exception.DuplicateResourceException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SettingsService settingsService;
    private final BranchScopeGuard branchScopeGuard;
    private final AuditService auditService;
    private final TaxRepository taxRepository;
    private final PricingService pricingService;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product with SKU: {} for shop: {}", request.getSku(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        if (productRepository.existsByShopIdAndSku(request.getShopId(), request.getSku())) {
            throw new DuplicateResourceException("SKU " + request.getSku() + " already exists in this shop");
        }
        Product product = new Product();
        applyRequest(product, request);
        product.setShop(shop);
        if (request.getCategoryId() != null) {
            product.setCategory(validateCategory(request.getCategoryId(), request.getShopId()));
        }
        if (product.getVatRate() == null) {
            product.setVatRate(settingsService.getVatRate(request.getShopId()));
        }
        if (product.getVatApplicable() == null) {
            product.setVatApplicable(true);
        }
        product.setStatus(request.getStatus() == null ? Status.ACTIVE : request.getStatus());
        Product saved = productRepository.save(product);
        auditService.log("CREATE", "Product", saved.getId().toString(), null, saved.getName() + " (SKU: " + saved.getSku() + ")");
        log.info("Product created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        log.info("Updating product: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), product.getShop().getId());
        if (request.getShopId() != null && !request.getShopId().equals(product.getShop().getId())) {
            throw new DuplicateResourceException("Product cannot be moved to another shop");
        }
        if (!product.getSku().equalsIgnoreCase(request.getSku())
                && productRepository.existsByShopIdAndSku(product.getShop().getId(), request.getSku())) {
            throw new DuplicateResourceException("SKU " + request.getSku() + " already exists in this shop");
        }
        String oldName = product.getName() + " (SKU: " + product.getSku() + ")";
        BigDecimal oldSellingPrice = product.getSellingPrice();
        applyRequest(product, request);
        if (request.getCategoryId() != null) {
            product.setCategory(validateCategory(request.getCategoryId(), product.getShop().getId()));
        } else {
            product.setCategory(null);
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (product.getVatRate() == null) {
            product.setVatRate(settingsService.getVatRate(product.getShop().getId()));
        }
        Product saved = productRepository.save(product);
        auditService.log("UPDATE", "Product", saved.getId().toString(), oldName, saved.getName() + " (SKU: " + saved.getSku() + ")");
        BigDecimal newSellingPrice = saved.getSellingPrice();
        if (oldSellingPrice == null ? newSellingPrice != null : newSellingPrice == null
                || oldSellingPrice.compareTo(newSellingPrice) != 0) {
            auditService.log("PRICE_CHANGE", "Product", saved.getId().toString(),
                    String.valueOf(oldSellingPrice), String.valueOf(newSellingPrice));
        }
        log.info("Product {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting / deactivating product: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), product.getShop().getId());
        boolean hasHistory = saleItemRepository.existsByProductId(id) || purchaseItemRepository.existsByProductId(id);
        Status newStatus = hasHistory ? Status.DISCONTINUED : Status.INACTIVE;
        product.setStatus(newStatus);
        productRepository.save(product);
        auditService.log("DELETE", "Product", product.getId().toString(), product.getName(), newStatus.name());
        log.info("Product {} marked as {}", id, newStatus);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        branchScopeGuard.requireShopAccessOrNotFound(SecurityUtils.currentUserId(), product.getShop().getId());
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(UUID shopId, String search, UUID categoryId, Status status, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Product> page = productRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("barcode")), like),
                        cb.like(cb.lower(root.get("brand")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductSearchResponse> search(UUID branchId, String query) {
        branchScopeGuard.requireBranchAccess(SecurityUtils.currentUserId(), branchId);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        UUID shopId = branch.getShop().getId();
        List<Product> products = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            String q = query.trim();
            products.addAll(productRepository.findAll((root, cq, cb) -> {
                String like = "%" + q.toLowerCase() + "%";
                return cb.and(
                        cb.equal(root.get("shop").get("id"), shopId),
                        cb.equal(root.get("status"), Status.ACTIVE),
                        cb.or(
                                cb.equal(root.get("sku"), q),
                                cb.equal(root.get("barcode"), q),
                                cb.like(cb.lower(root.get("name")), like)));
            }, Pageable.unpaged()).getContent());
        }
        return products.stream().map(p -> {
            BigDecimal qty = BigDecimal.ZERO;
            String stockStatus = "OUT_OF_STOCK";
            var inventoryOpt = inventoryRepository.findByBranchIdAndProductId(branchId, p.getId());
            if (inventoryOpt.isPresent()) {
                qty = inventoryOpt.get().getQuantityAvailable();
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    stockStatus = qty.compareTo(p.getReorderLevel()) <= 0 ? "LOW_STOCK" : "IN_STOCK";
                }
            }
            BigDecimal finalQty = qty;
            String finalStatus = stockStatus;
            return ProductSearchResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .barcode(p.getBarcode())
                .brand(p.getBrand())
                .unit(p.getUnit())
                .sellingPrice(p.getSellingPrice())
                .vatRate(p.getVatRate())
                .vatApplicable(p.getVatApplicable())
                .status(p.getStatus())
                .quantityAvailable(finalQty)
                .stockStatus(finalStatus)
                .build();
        }).toList();
    }

    public Product getEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category validateCategory(UUID categoryId, UUID shopId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        if (!category.getShop().getId().equals(shopId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return category;
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit() == null ? "PCS" : request.getUnit());
        product.setDescription(request.getDescription());
        product.setPurchasePrice(request.getPurchasePrice().setScale(2, RoundingMode.HALF_UP));
        product.setSellingPrice(request.getSellingPrice().setScale(2, RoundingMode.HALF_UP));
        product.setVatApplicable(request.getVatApplicable() == null || request.getVatApplicable());
        product.setVatRate(request.getVatRate() == null ? settingsService.getVatRate(product.getShop() == null
                ? request.getShopId() : product.getShop().getId()) : request.getVatRate());
        product.setReorderLevel(request.getReorderLevel() == null
                ? BigDecimal.ZERO : request.getReorderLevel().setScale(2, RoundingMode.HALF_UP));
        product.setImageUrl(request.getImageUrl());
        // Wire dynamic Tax entity if provided
        if (request.getTaxId() != null) {
            Tax tax = taxRepository.findById(request.getTaxId())
                    .orElseThrow(() -> new com.smartshop.shared.exception.ResourceNotFoundException("Tax", request.getTaxId()));
            product.setTax(tax);
        } else {
            product.setTax(null);
        }
    }

    private ProductResponse toResponse(Product product) {
        List<String> warnings = new ArrayList<>();
        if (product.getSellingPrice().compareTo(product.getPurchasePrice()) < 0) {
            warnings.add("Selling price is below purchase price");
        }
        BigDecimal effectiveCost = pricingService.effectiveCost(product, product.getShop().getId());
        BigDecimal suggestedSellingPrice = pricingService.suggestSellingPrice(product, null);
        BigDecimal profitMarginPercent = null;
        if (product.getSellingPrice() != null && product.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            profitMarginPercent = product.getSellingPrice().subtract(effectiveCost)
                    .multiply(new BigDecimal("100"))
                    .divide(product.getSellingPrice(), 2, RoundingMode.HALF_UP);
        }
        return ProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShop().getId())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .unit(product.getUnit())
                .description(product.getDescription())
                .purchasePrice(product.getPurchasePrice())
                .sellingPrice(product.getSellingPrice())
                .effectiveCost(effectiveCost)
                .suggestedSellingPrice(suggestedSellingPrice)
                .profitMarginPercent(profitMarginPercent)
                .vatApplicable(product.getVatApplicable())
                .vatRate(product.getVatRate())
                .taxId(product.getTax() == null ? null : product.getTax().getId())
                .taxName(product.getTax() == null ? null : product.getTax().getName())
                .reorderLevel(product.getReorderLevel())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .warnings(warnings)
                .createdAt(product.getCreatedAt())
                .build();
    }
}