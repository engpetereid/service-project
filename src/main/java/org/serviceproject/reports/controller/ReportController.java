package org.serviceproject.reports.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.reports.dto.AttendanceReportFilter;
import org.serviceproject.reports.dto.ConfessionReportFilter;
import org.serviceproject.reports.dto.StudentReportFilter;
import org.serviceproject.reports.dto.VisitReportFilter;
import org.serviceproject.reports.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for generating and downloading UTF-8 Arabic CSV reports.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/export/students")
    public ResponseEntity<byte[]> exportStudents(
            @ModelAttribute StudentReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.exportStudentsCsv(filter, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/visits")
    public ResponseEntity<byte[]> exportVisits(
            @ModelAttribute VisitReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.exportVisitsCsv(filter, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visits_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/attendance")
    public ResponseEntity<byte[]> exportAttendance(
            @ModelAttribute AttendanceReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.exportAttendanceCsv(filter, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/export/confession")
    public ResponseEntity<byte[]> exportConfession(
            @ModelAttribute ConfessionReportFilter filter,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.exportConfessionsCsv(filter, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"confessions_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
