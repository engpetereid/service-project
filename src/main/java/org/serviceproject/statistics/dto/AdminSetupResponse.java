package org.serviceproject.statistics.dto;

import java.util.List;

public record AdminSetupResponse(
        int totalMinistries,
        int totalClasses,
        int totalServants,
        int totalStudents,
        int servantsWithAccount,
        int servantsWithoutAccount,
        int ministriesWithoutSecretary,
        int classesWithoutSecretary,
        int studentsWithoutServant,
        boolean hasMinistry,
        boolean hasClasses,
        boolean hasServiceSecretary,
        boolean hasClassSecretary,
        boolean hasServants,
        boolean hasStudents,
        boolean hasAssignments,
        boolean setupComplete,
        List<ConfigurationWarning> warnings
) {
    public record ConfigurationWarning(
            String code,
            String title,
            String message,
            String severity,
            String actionLabel,
            String actionUrl,
            int count
    ) {}
}
