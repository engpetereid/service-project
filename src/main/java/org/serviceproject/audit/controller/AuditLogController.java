package org.serviceproject.audit.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.audit.dto.AuditLogFilter;
import org.serviceproject.audit.dto.AuditLogResponse;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.service.AuditService;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.security.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for querying audit logs (General Admin only).
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        AuditLogFilter filter = new AuditLogFilter(entityType, entityId, actorUserId, action, startDate, endDate, page, size);
        return ResponseEntity.ok(auditService.findLogs(filter, principal));
    }
}
