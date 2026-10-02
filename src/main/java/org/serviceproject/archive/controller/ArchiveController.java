package org.serviceproject.archive.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.archive.dto.ArchiveSummaryResponse;
import org.serviceproject.archive.dto.DeletedPersonResponse;
import org.serviceproject.archive.service.ArchiveService;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.weeks.dto.WeekResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the archive category views and historical data summaries.
 */
@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * Lists all soft-deleted people (General Admin only).
     */
    @GetMapping("/deleted-people")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<List<DeletedPersonResponse>> getDeletedPeople(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeletedPersonResponse> response = archiveService.getDeletedPeople(principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all locked weeks older than 30 days.
     */
    @GetMapping("/weeks")
    public ResponseEntity<List<WeekResponse>> getLockedWeeks(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<WeekResponse> response = archiveService.getLockedWeeks(principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves archive summary counts tailored to the user's authorization level.
     */
    @GetMapping("/summary")
    public ResponseEntity<ArchiveSummaryResponse> getArchiveSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        ArchiveSummaryResponse response = archiveService.getArchiveSummary(principal);
        return ResponseEntity.ok(response);
    }
}
