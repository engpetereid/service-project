package org.serviceproject.audit.dto;

import org.serviceproject.audit.entity.AuditAction;

import java.time.LocalDateTime;

/**
 * Response DTO for audit log queries.
 */
public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorName,
        AuditAction action,
        String entityType,
        Long entityId,
        String oldValues,
        String newValues,
        String clientIp,
        LocalDateTime timestamp
) {}
