package com.smartshop.features.branch.controller;

import com.smartshop.features.branch.dto.BranchRequest;
import com.smartshop.features.branch.dto.BranchResponse;
import com.smartshop.features.branch.service.BranchService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listBranches(@RequestParam UUID shopId) {
        return ResponseEntity.ok(ApiResponse.success(branchService.listBranches(shopId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranch(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", branchService.createBranch(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(@PathVariable UUID id,
                                                                    @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", branchService.updateBranch(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Branch deactivated successfully", null));
    }
}