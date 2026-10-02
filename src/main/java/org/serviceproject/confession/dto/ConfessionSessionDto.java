package org.serviceproject.confession.dto;

import java.time.LocalDate;
import java.util.List;

public record ConfessionSessionDto(
        String sessionId,
        LocalDate sessionDate,
        String confessionFather,
        int studentCount,
        String notes,
        String recordedByName,
        List<SessionStudentDto> students
) {
    public record SessionStudentDto(
            Long studentId,
            String studentName,
            String phone,
            String className,
            Long recordId
    ) {}
}
