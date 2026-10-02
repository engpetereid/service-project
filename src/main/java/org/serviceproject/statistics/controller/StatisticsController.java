package org.serviceproject.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.statistics.dto.AbsenceAlertResponse;
import org.serviceproject.statistics.dto.AdminSetupResponse;
import org.serviceproject.statistics.dto.ClassStatisticsResponse;
import org.serviceproject.statistics.dto.DashboardStatisticsResponse;
import org.serviceproject.statistics.dto.MinistryStatisticsResponse;
import org.serviceproject.statistics.dto.ServantStatisticsResponse;
import org.serviceproject.statistics.dto.StudentStatisticsResponse;
import org.serviceproject.statistics.dto.WeeklyTrendDataPoint;
import org.serviceproject.statistics.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for dynamic statistics, dashboards, and absence alerts.
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/admin-setup")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<AdminSetupResponse> getAdminSetup(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getAdminSetupStatus(principal));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatisticsResponse> getDashboard(
            @RequestParam(required = false) Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getDashboard(weekId, principal));
    }

    @GetMapping("/ministry/{id}")
    public ResponseEntity<MinistryStatisticsResponse> getMinistryStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getMinistryStatistics(id, weekId, principal));
    }

    @GetMapping("/class/{id}")
    public ResponseEntity<ClassStatisticsResponse> getClassStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getClassStatistics(id, weekId, principal));
    }

    @GetMapping("/servant/{id}")
    public ResponseEntity<ServantStatisticsResponse> getServantStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getServantStatistics(id, weekId, principal));
    }

    @GetMapping("/absence-alerts")
    public ResponseEntity<List<AbsenceAlertResponse>> getAbsenceAlerts(
            @RequestParam(defaultValue = "2") int threshold,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getAbsenceAlerts(principal, threshold));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<WeeklyTrendDataPoint>> getTrends(
            @RequestParam(required = false) Integer count,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getTrends(count, principal));
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<StudentStatisticsResponse> getStudentStatistics(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statisticsService.getStudentStatistics(id, principal));
    }
}
