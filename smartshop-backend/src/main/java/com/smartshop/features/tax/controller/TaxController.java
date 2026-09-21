package com.smartshop.features.tax.controller;

import com.smartshop.features.tax.dto.*;
import com.smartshop.features.tax.service.TaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/taxes")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    /**
     * POST /api/v1/taxes
     * Create a new tax for a shop. Shop admin only.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<TaxResponse> create(@Valid @RequestBody TaxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.create(request));
    }

    /**
     * GET /api/v1/taxes/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<TaxResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(taxService.get(id));
    }

    /**
     * GET /api/v1/taxes?shopId=&activeOnly=true
     * List taxes for a shop, optionally filtering to active-only.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<List<TaxResponse>> list(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(taxService.listByShop(shopId, activeOnly));
    }

    /**
     * PUT /api/v1/taxes/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<TaxResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaxRequest request) {
        return ResponseEntity.ok(taxService.update(id, request));
    }

    /**
     * DELETE /api/v1/taxes/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taxService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Tax Rate endpoints ────────────────────────────────────────────────────

    /**
     * POST /api/v1/taxes/{id}/rates
     * Add a new effective-date rate to an existing tax.
     */
    @PostMapping("/{id}/rates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    public ResponseEntity<TaxRateResponse> addRate(
            @PathVariable UUID id,
            @Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.addRate(id, request));
    }

    /**
     * GET /api/v1/taxes/{id}/rates
     * List all rate history for a tax.
     */
    @GetMapping("/{id}/rates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','ACCOUNTANT')")
    public ResponseEntity<List<TaxRateResponse>> getRates(@PathVariable UUID id) {
        return ResponseEntity.ok(taxService.getRates(id));
    }
}
