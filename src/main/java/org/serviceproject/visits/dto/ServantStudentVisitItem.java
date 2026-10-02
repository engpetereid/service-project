package org.serviceproject.visits.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An assigned student with their visit status for the current week.
 */
public record ServantStudentVisitItem(
        Long studentId,
        String fullName,
        String phone,
        String address,
        String guardianPhone,
        VisitSummaryDto visit,
        Long classId,
        String className,
        Long servantId,
        String servantName
) {
    public ServantStudentVisitItem(
            Long studentId,
            String fullName,
            String phone,
            String address,
            String guardianPhone,
            VisitSummaryDto visit
    ) {
        this(studentId, fullName, phone, address, guardianPhone, visit, null, null, null, null);
    }

    @JsonProperty("studentName")
    public String studentName() {
        return fullName;
    }

    @JsonProperty("visited")
    public boolean isVisited() {
        return visit != null;
    }
}
