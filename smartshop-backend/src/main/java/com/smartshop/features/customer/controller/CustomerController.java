package com.smartshop.features.customer.controller;

import com.smartshop.features.customer.dto.CustomerRequest;
import com.smartshop.features.customer.dto.CustomerResponse;
import com.smartshop.features.customer.service.CustomerService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "asc"), sortParts[0]));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(customerService.list(shopId, search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }
}