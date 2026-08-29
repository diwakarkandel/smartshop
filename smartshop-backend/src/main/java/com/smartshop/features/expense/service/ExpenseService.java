package com.smartshop.features.expense.service;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.expense.dto.ExpenseRequest;
import com.smartshop.features.expense.dto.ExpenseResponse;
import com.smartshop.features.expense.entity.Expense;
import com.smartshop.features.expense.repository.ExpenseRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        log.info("Creating expense '{}' for amount {} in shop {}", request.getTitle(), request.getAmount(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
            if (!branch.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Branch does not belong to the given shop");
            }
        }
        User createdBy = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        Expense expense = new Expense();
        expense.setShop(shop);
        expense.setBranch(branch);
        expense.setTitle(request.getTitle());
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        expense.setExpenseDate(request.getExpenseDate() == null ? LocalDate.now() : request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNote(request.getNote());
        expense.setCreatedBy(createdBy);
        Expense saved = expenseRepository.save(expense);
        log.info("Expense created with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        log.info("Updating expense: {}", id);
        Expense expense = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), expense.getShop().getId());
        expense.setTitle(request.getTitle());
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        expense.setExpenseDate(request.getExpenseDate() == null ? expense.getExpenseDate() : request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNote(request.getNote());
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
            if (!branch.getShop().getId().equals(expense.getShop().getId())) {
                throw new BadRequestException("Branch does not belong to the given shop");
            }
            expense.setBranch(branch);
        } else {
            expense.setBranch(null);
        }
        Expense saved = expenseRepository.save(expense);
        log.info("Expense {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting expense: {}", id);
        Expense expense = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), expense.getShop().getId());
        expenseRepository.delete(expense);
        log.info("Expense {} deleted", id);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        Expense expense = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), expense.getShop().getId());
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(UUID shopId, UUID branchId, String category, LocalDate dateFrom,
                                      LocalDate dateTo, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Expense> page = expenseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), dateTo));
            }
            query.orderBy(cb.desc(root.get("expenseDate")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    public Expense getEntity(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .shopId(expense.getShop().getId())
                .branchId(expense.getBranch() == null ? null : expense.getBranch().getId())
                .branchName(expense.getBranch() == null ? null : expense.getBranch().getName())
                .title(expense.getTitle())
                .category(expense.getCategory())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .note(expense.getNote())
                .createdById(expense.getCreatedBy().getId())
                .createdByName(expense.getCreatedBy().getFullName())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}