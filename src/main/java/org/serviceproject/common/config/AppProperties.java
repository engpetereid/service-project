package org.serviceproject.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;

/**
 * Application-level configuration properties bound from the {@code app.*} namespace.
 * <p>
 * Centralizes all business-configurable defaults so they are never scattered
 * throughout the codebase.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        /** IANA timezone id (e.g. "Africa/Cairo") used for all date/time calculations. */
        String timeZone,

        /** Day of the week that starts a new church week (default: FRIDAY). */
        DayOfWeek weekStart,

        /** Number of days after a week ends before it becomes locked for edits. */
        int weekLockDays,

        /** Hours before week-end when unvisited-student reminders are sent. */
        int weeklyReminderHours
) {}
