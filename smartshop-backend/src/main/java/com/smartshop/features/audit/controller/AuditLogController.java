package com.smartshop.features.audit.controller;

import com.smartshop.features.audit.dto.AuditLogResponse;
import com.smartshop.features.audit.service.AuditService;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "System audit trail – who did what and when")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    @Operation(summary = "List audit logs with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> list(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SIZE) int size,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        log.info("Fetching audit logs: action={}, entityName={}, userId={}", action, entityName, userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                auditService.list(userId, action, entityName, dateFrom, dateTo, pageable))));
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SHOP_ADMIN')")
    @Operation(summary = "Get full audit history for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getEntityHistory(
            @PathVariable String entityName,
            @PathVariable String entityId) {
        log.info("Fetching audit history for {}:{}", entityName, entityId);
        return ResponseEntity.ok(ApiResponse.success(
                auditService.getEntityHistory(entityName, entityId)));
    }
}