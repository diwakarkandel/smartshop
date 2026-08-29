package com.smartshop.features.category.service;

import com.smartshop.features.category.dto.CategoryRequest;
import com.smartshop.features.category.dto.CategoryResponse;
import com.smartshop.features.category.entity.Category;
import com.smartshop.features.category.repository.CategoryRepository;
import com.smartshop.features.product.repository.ProductRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.Status;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        log.info("Creating category: {} for shop: {}", request.getName(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        if (request.getCode() != null && categoryRepository.existsByShopIdAndCode(request.getShopId(), request.getCode())) {
            throw new DuplicateResourceException("Category code " + request.getCode() + " already exists in this shop");
        }
        Category category = new Category();
        applyRequest(category, request);
        category.setShop(shop);
        if (request.getParentId() != null) {
            category.setParent(getEntity(request.getParentId()));
        }
        category.setStatus(request.getStatus() == null ? Status.ACTIVE : request.getStatus());
        Category saved = categoryRepository.save(category);
        log.info("Category {} created with id: {}", saved.getName(), saved.getId());
        return toResponse(saved, false);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        log.info("Updating category: {}", id);
        Category category = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), category.getShop().getId());
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(category.getCode())
                && categoryRepository.existsByShopIdAndCode(category.getShop().getId(), request.getCode())) {
            throw new DuplicateResourceException("Category code " + request.getCode() + " already exists in this shop");
        }
        applyRequest(category, request);
        if (request.getParentId() != null) {
            validateParentChain(category.getId(), request.getParentId());
            category.setParent(getEntity(request.getParentId()));
        } else {
            category.setParent(null);
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        Category saved = categoryRepository.save(category);
        log.info("Category {} updated successfully", id);
        return toResponse(saved, false);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deactivating category: {}", id);
        Category category = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), category.getShop().getId());
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Category has products and cannot be deactivated");
        }
        category.setStatus(Status.INACTIVE);
        categoryRepository.save(category);
        log.info("Category {} deactivated", id);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> tree(UUID shopId) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        List<Category> all = categoryRepository.findByShopIdOrderByNameAsc(shopId);
        Map<UUID, CategoryResponse> nodeMap = new HashMap<>();
        for (Category category : all) {
            nodeMap.put(category.getId(), toResponse(category, true));
        }
        List<CategoryResponse> roots = new ArrayList<>();
        for (Category category : all) {
            CategoryResponse node = nodeMap.get(category.getId());
            if (category.getParent() != null && nodeMap.containsKey(category.getParent().getId())) {
                CategoryResponse.appendChild(nodeMap.get(category.getParent().getId()), node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(UUID shopId, String search, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Category> page = categoryRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
        return page.map(c -> toResponse(c, false));
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        Category category = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), category.getShop().getId());
        return toResponse(category, false);
    }

    public Category getEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void validateParentChain(UUID categoryId, UUID newParentId) {
        if (categoryId.equals(newParentId)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        Set<UUID> visited = new HashSet<>();
        UUID current = newParentId;
        while (current != null) {
            if (categoryId.equals(current)) {
                throw new BadRequestException("Circular parent reference detected");
            }
            if (!visited.add(current)) {
                throw new BadRequestException("Circular parent reference detected");
            }
            Category parent = categoryRepository.findById(current).orElse(null);
            if (parent == null) {
                break;
            }
            current = parent.getParent() == null ? null : parent.getParent().getId();
        }
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
    }

    private CategoryResponse toResponse(Category category, boolean includeChildren) {
        return CategoryResponse.builder()
                .id(category.getId())
                .shopId(category.getShop().getId())
                .parentId(category.getParent() == null ? null : category.getParent().getId())
                .name(category.getName())
                .code(category.getCode())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .children(includeChildren ? new ArrayList<>() : null)
                .build();
    }
}