package org.serviceproject.audit.dto;

import org.serviceproject.audit.entity.AuditAction;

import java.time.LocalDate;

/**
 * Filter criteria for querying audit logs.
 */
public record AuditLogFilter(
        String entityType,
        Long entityId,
        Long actorUserId,
        AuditAction action,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer size
) {
    public int getResolvedPage() {
        return (page != null && page >= 0) ? page : 0;
    }

    public int getResolvedSize() {
        return (size != null && size > 0 && size <= 100) ? size : 20;
    }
}
