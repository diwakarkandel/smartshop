package com.smartshop.features.customer.service;

import com.smartshop.features.customer.dto.CustomerRequest;
import com.smartshop.features.customer.dto.CustomerResponse;
import com.smartshop.features.customer.entity.Customer;
import com.smartshop.features.customer.repository.CustomerRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ShopRepository shopRepository;
    private final BranchScopeGuard branchScopeGuard;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        log.info("Creating customer {} in shop {}", request.getName(), request.getShopId());
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));
        Customer customer = new Customer();
        applyRequest(customer, request);
        customer.setShop(shop);
        Customer saved = customerRepository.save(customer);
        log.info("Customer {} created with id: {}", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        log.info("Updating customer {}", id);
        Customer customer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), customer.getShop().getId());
        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);
        log.info("Customer {} updated successfully", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting customer {}", id);
        Customer customer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), customer.getShop().getId());
        customerRepository.delete(customer);
        log.info("Customer {} deleted", id);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        Customer customer = getEntity(id);
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), customer.getShop().getId());
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(UUID shopId, String search, Pageable pageable) {
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), shopId);
        Page<Customer> page = customerRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("phone")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(this::toResponse);
    }

    public Customer getEntity(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .shopId(customer.getShop().getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}