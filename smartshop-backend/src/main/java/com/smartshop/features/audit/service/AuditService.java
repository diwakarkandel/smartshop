package com.smartshop.features.audit.service;

import com.smartshop.features.audit.dto.AuditLogResponse;
import com.smartshop.features.audit.entity.AuditLog;
import com.smartshop.features.audit.repository.AuditLogRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String action, String entityName, String entityId, String oldValue, String newValue) {
        log.debug("Audit record: action={}, entityName={}, entityId={}", action, entityName, entityId);
        AuditLog auditLog = AuditLog.builder()
                .user(currentUserOrNull())
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(resolveIp())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(UUID userId, String action, String entityName, LocalDateTime dateFrom,
                                       LocalDateTime dateTo, Pageable pageable) {
        log.info("Querying audit logs: userId={}, action={}, entityName={}", userId, action, entityName);
        Page<AuditLog> page = auditLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (entityName != null && !entityName.isBlank()) {
                predicates.add(cb.equal(root.get("entityName"), entityName));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        return page.map(l -> AuditLogResponse.builder()
                .id(l.getId())
                .userId(l.getUser() == null ? null : l.getUser().getId())
                .userName(l.getUser() == null ? null : l.getUser().getFullName())
                .action(l.getAction())
                .entityName(l.getEntityName())
                .entityId(l.getEntityId())
                .oldValue(l.getOldValue())
                .newValue(l.getNewValue())
                .ipAddress(l.getIpAddress())
                .createdAt(l.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityName, String entityId) {
        log.info("Fetching audit history for entity {}:{}", entityName, entityId);
        return auditLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("entityName"), entityName));
            predicates.add(cb.equal(root.get("entityId"), entityId));
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }).stream().map(l -> AuditLogResponse.builder()
                .id(l.getId())
                .userId(l.getUser() == null ? null : l.getUser().getId())
                .userName(l.getUser() == null ? null : l.getUser().getFullName())
                .action(l.getAction())
                .entityName(l.getEntityName())
                .entityId(l.getEntityId())
                .oldValue(l.getOldValue())
                .newValue(l.getNewValue())
                .ipAddress(l.getIpAddress())
                .createdAt(l.getCreatedAt())
                .build()).toList();
    }

    private User currentUserOrNull() {
        try {
            return userRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }
}