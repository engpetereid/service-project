package org.serviceproject.notifications.entity;

/**
 * Types of in-app notifications generated across the application.
 */
public enum NotificationType {
    /** Weekly reminder for servants with pending visits before week ends. */
    WEEKLY_REMINDER,

    /** Alert when a student is absent for consecutive weeks. */
    ABSENCE_ALERT,

    /** General administrative or system broadcast. */
    SYSTEM
}
