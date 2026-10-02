package org.serviceproject.audit.entity;

/**
 * Enumeration of auditable actions in the system.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    RESTORE,
    PROMOTION,
    ROLE_CHANGE,
    STATUS_CHANGE,
    LOCK_OVERRIDE,
    PASSWORD_CHANGE,
    SETTING_CHANGE
}
