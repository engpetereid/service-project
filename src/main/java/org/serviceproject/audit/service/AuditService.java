package org.serviceproject.audit.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.audit.dto.AuditLogFilter;
import org.serviceproject.audit.dto.AuditLogResponse;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.entity.AuditLog;
import org.serviceproject.audit.repository.AuditLogRepository;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.HttpUtil;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing audit logs for critical entity lifecycles and administrative changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    /**
     * Records an audit log entry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse log(UserPrincipal principal, AuditAction action, String entityType,
                                Long entityId, Object oldValues, Object newValues, String clientIp) {
        UserAccount actor = null;
        String actorName = "SYSTEM";

        if (principal != null) {
            actorName = principal.getUsername();
            if (principal.getUserId() != null) {
                actor = userAccountRepository.findById(principal.getUserId()).orElse(null);
                if (actor != null && actor.getPerson() != null && actor.getPerson().getFullName() != null) {
                    actorName = actor.getPerson().getFullName();
                }
            }
        }

        String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : HttpUtil.getClientIp();
        String oldJson = toJson(oldValues);
        String newJson = toJson(newValues);

        AuditLog auditLog = new AuditLog(actor, actorName, action, entityType, entityId, oldJson, newJson, ip);
        AuditLog saved = auditLogRepository.save(auditLog);

        log.info("Audit log recorded: action={}, entityType={}, entityId={}, actor={}", action, entityType, entityId, actorName);

        return toResponse(saved);
    }

    /**
     * Convenience method for system operations.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse logSystem(AuditAction action, String entityType, Long entityId,
                                      Object oldValues, Object newValues) {
        return log(null, action, entityType, entityId, oldValues, newValues, null);
    }

    /**
     * Queries audit logs with dynamic filtering and pagination (General Admin only).
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findLogs(AuditLogFilter filter, UserPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw AppException.forbidden("ACCESS_DENIED", "فقط الأمين العام يمكنه عرض سجلات التدقيق");
        }

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.entityType() != null && !filter.entityType().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), filter.entityType().trim().toUpperCase()));
            }

            if (filter.entityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), filter.entityId()));
            }

            if (filter.actorUserId() != null) {
                predicates.add(cb.equal(root.get("actor").get("id"), filter.actorUserId()));
            }

            if (filter.action() != null) {
                predicates.add(cb.equal(root.get("action"), filter.action()));
            }

            if (filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.startDate().atStartOfDay()));
            }

            if (filter.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.endDate().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                filter.getResolvedPage(),
                filter.getResolvedSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActor() != null ? auditLog.getActor().getId() : null,
                auditLog.getActorName(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOldValues(),
                auditLog.getNewValues(),
                auditLog.getClientIp(),
                auditLog.getCreatedAt()
        );
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize audit data to JSON", e);
            return String.valueOf(obj);
        }
    }
}
