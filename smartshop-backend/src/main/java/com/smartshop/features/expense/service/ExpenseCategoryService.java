package com.smartshop.features.expense.service;

import com.smartshop.features.expense.dto.ExpenseCategoryResponse;
import com.smartshop.features.expense.entity.ExpenseCategory;
import com.smartshop.features.expense.repository.ExpenseCategoryRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ForbiddenException;
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
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ShopRepository shopRepository;
    private final BranchScopeGuard branchScopeGuard;

    /**
     * Authorizes a mutation on an expense category with the given owning shop.
     * Shop-scoped categories require access to that shop; global categories
     * (shopId == null) may only be managed by a platform SUPER_ADMIN.
     */
    private void authorizeMutation(UUID shopId) {
        if (shopId != null) {
            branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        } else if (!SecurityUtils.currentUser().hasRole(AppConstants.ROLE_SUPER_ADMIN)) {
            throw new ForbiddenException("Only a platform administrator can manage global expense categories");
        }
    }

    /** Returns global categories + shop-specific categories for the given shop. */
    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> list(UUID shopId) {
        List<ExpenseCategory> categories = shopId != null
                ? expenseCategoryRepository.findByShopIdOrGlobal(shopId)
                : expenseCategoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExpenseCategoryResponse create(UUID shopId, String name, String description) {
        authorizeMutation(shopId);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        String trimmed = name.trim();
        // Check uniqueness within the shop scope
        if (shopId != null && expenseCategoryRepository.existsByShopIdAndNameIgnoreCase(shopId, trimmed)) {
            throw new BadRequestException("Expense category already exists for this shop: " + name);
        }
        if (shopId == null && expenseCategoryRepository.existsByShopIsNullAndNameIgnoreCase(trimmed)) {
            throw new BadRequestException("Global expense category already exists: " + name);
        }
        Shop shop = null;
        if (shopId != null) {
            shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));
        }
        ExpenseCategory category = new ExpenseCategory();
        category.setShop(shop);
        category.setName(trimmed);
        category.setDescription(description == null || description.isBlank() ? null : description.trim());
        ExpenseCategory saved = expenseCategoryRepository.save(category);
        log.info("Expense category created: {} for shop: {}", saved.getName(), shopId);
        return toResponse(saved);
    }

    @Transactional
    public ExpenseCategoryResponse update(UUID id, String name, String description) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
        authorizeMutation(category.getShop() != null ? category.getShop().getId() : null);
        if (name != null && !name.isBlank() && !category.getName().equalsIgnoreCase(name.trim())) {
            String trimmed = name.trim();
            UUID shopId = category.getShop() != null ? category.getShop().getId() : null;
            boolean conflict = shopId != null
                    ? expenseCategoryRepository.existsByShopIdAndNameIgnoreCase(shopId, trimmed)
                    : expenseCategoryRepository.existsByShopIsNullAndNameIgnoreCase(trimmed);
            if (conflict) {
                throw new BadRequestException("Expense category already exists: " + name);
            }
            category.setName(trimmed);
        }
        if (description != null) {
            category.setDescription(description.isBlank() ? null : description.trim());
        }
        ExpenseCategory saved = expenseCategoryRepository.save(category);
        log.info("Expense category updated: {}", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
        authorizeMutation(category.getShop() != null ? category.getShop().getId() : null);
        expenseCategoryRepository.delete(category);
        log.info("Expense category deleted: {}", category.getName());
    }

    private ExpenseCategoryResponse toResponse(ExpenseCategory category) {
        return ExpenseCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}