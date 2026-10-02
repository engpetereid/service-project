package org.serviceproject.reports.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.reports.dto.AttendanceReportFilter;
import org.serviceproject.reports.dto.ConfessionReportFilter;
import org.serviceproject.reports.dto.StudentReportFilter;
import org.serviceproject.reports.dto.VisitReportFilter;
import org.serviceproject.reports.service.ReportService;
import org.serviceproject.users.entity.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));
    }

    @Test
    void exportStudents_returnsCsvAttachment() {
        StudentReportFilter filter = new StudentReportFilter(1L, null, null, null, null);
        byte[] csvData = "\uFEFFheader1,header2\nval1,val2".getBytes(StandardCharsets.UTF_8);

        when(reportService.exportStudentsCsv(filter, principal)).thenReturn(csvData);

        ResponseEntity<byte[]> response = reportController.exportStudents(filter, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"students_report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.parseMediaType("text/csv; charset=UTF-8"), response.getHeaders().getContentType());
        assertArrayEquals(csvData, response.getBody());
    }

    @Test
    void exportVisits_returnsCsvAttachment() {
        VisitReportFilter filter = new VisitReportFilter(1L, 2L, null, null, null);
        byte[] csvData = "\uFEFFvisit1,visit2".getBytes(StandardCharsets.UTF_8);

        when(reportService.exportVisitsCsv(filter, principal)).thenReturn(csvData);

        ResponseEntity<byte[]> response = reportController.exportVisits(filter, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"visits_report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.parseMediaType("text/csv; charset=UTF-8"), response.getHeaders().getContentType());
        assertArrayEquals(csvData, response.getBody());
    }

    @Test
    void exportAttendance_returnsCsvAttachment() {
        AttendanceReportFilter filter = new AttendanceReportFilter(1L, null, 2L, null);
        byte[] csvData = "\uFEFFattendance1,attendance2".getBytes(StandardCharsets.UTF_8);

        when(reportService.exportAttendanceCsv(filter, principal)).thenReturn(csvData);

        ResponseEntity<byte[]> response = reportController.exportAttendance(filter, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"attendance_report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.parseMediaType("text/csv; charset=UTF-8"), response.getHeaders().getContentType());
        assertArrayEquals(csvData, response.getBody());
    }

    @Test
    void exportConfession_returnsCsvAttachment() {
        ConfessionReportFilter filter = new ConfessionReportFilter(1L, null, null, null, null, null);
        byte[] csvData = "\uFEFFconfession1,confession2".getBytes(StandardCharsets.UTF_8);

        when(reportService.exportConfessionsCsv(filter, principal)).thenReturn(csvData);

        ResponseEntity<byte[]> response = reportController.exportConfession(filter, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"confessions_report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.parseMediaType("text/csv; charset=UTF-8"), response.getHeaders().getContentType());
        assertArrayEquals(csvData, response.getBody());
    }
}
