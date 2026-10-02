package org.serviceproject.attendance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.attendance.dto.AttendanceRecordResponse;
import org.serviceproject.attendance.dto.AttendanceSessionResponse;
import org.serviceproject.attendance.dto.BatchToggleAttendanceRequest;
import org.serviceproject.attendance.dto.CreateSessionRequest;
import org.serviceproject.attendance.dto.SessionDetailResponse;
import org.serviceproject.attendance.dto.ToggleAttendanceRequest;
import org.serviceproject.attendance.service.AttendanceService;
import org.serviceproject.common.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing attendance sessions and marking attendance.
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/sessions")
    public ResponseEntity<List<AttendanceSessionResponse>> getSessionsByWeek(@RequestParam Long weekId) {
        return ResponseEntity.ok(attendanceService.getSessionsByWeek(weekId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<AttendanceSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AttendanceSessionResponse response = attendanceService.createSession(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionDetailResponse> getSessionDetails(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getSessionDetails(id));
    }

    @PostMapping("/records")
    public ResponseEntity<AttendanceRecordResponse> toggleAttendance(
            @Valid @RequestBody ToggleAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.toggleAttendance(request, principal));
    }

    @PostMapping("/records/batch")
    public ResponseEntity<List<AttendanceRecordResponse>> batchToggleAttendance(
            @Valid @RequestBody BatchToggleAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.batchToggleAttendance(request, principal));
    }
}
