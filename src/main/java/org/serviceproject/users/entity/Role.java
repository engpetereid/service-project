package org.serviceproject.users.entity;

/**
 * System roles.
 * <p>
 * Arabic labels:
 * <ul>
 *   <li>GENERAL_ADMIN → الأمين العام</li>
 *   <li>SERVICE_SECRETARY → أمين الخدمة</li>
 *   <li>CLASS_SECRETARY → أمين الفصل</li>
 *   <li>SERVANT → الخادم</li>
 * </ul>
 * A person can hold multiple roles simultaneously (requirement #6).
 */
public enum Role {
    GENERAL_ADMIN,
    SERVICE_SECRETARY,
    CLASS_SECRETARY,
    SERVANT
}
