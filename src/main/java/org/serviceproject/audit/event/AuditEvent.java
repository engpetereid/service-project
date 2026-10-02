package org.serviceproject.audit.event;

import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.common.security.UserPrincipal;

/**
 * Domain event dispatched for auditable actions.
 */
public record AuditEvent(
        UserPrincipal principal,
        AuditAction action,
        String entityType,
        Long entityId,
        Object oldValues,
        Object newValues,
        String clientIp
) {
    public static AuditEvent of(UserPrincipal principal, AuditAction action, String entityType, Long entityId,
                                Object oldValues, Object newValues) {
        return new AuditEvent(principal, action, entityType, entityId, oldValues, newValues, null);
    }

    public static AuditEvent system(AuditAction action, String entityType, Long entityId,
                                    Object oldValues, Object newValues) {
        return new AuditEvent(null, action, entityType, entityId, oldValues, newValues, null);
    }
}
