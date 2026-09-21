package com.smartshop.features.expense.controller;

import com.smartshop.features.expense.dto.ExpenseCategoryResponse;
import com.smartshop.features.expense.service.ExpenseCategoryService;
import com.smartshop.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','ACCOUNTANT','CASHIER')")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryResponse>>> list(
            @RequestParam(required = false) UUID shopId) {
        return ResponseEntity.ok(ApiResponse.success(expenseCategoryService.list(shopId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> create(
            @RequestParam(required = false) UUID shopId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense category created successfully",
                        expenseCategoryService.create(shopId, body.get("name"), body.get("description"))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> update(@PathVariable UUID id,
                                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Expense category updated successfully",
                expenseCategoryService.update(id, body.get("name"), body.get("description"))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        expenseCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Expense category deleted successfully", null));
    }
}