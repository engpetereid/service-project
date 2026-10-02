package org.serviceproject.confession.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.confession.dto.ConfessionResponse;
import org.serviceproject.confession.dto.ConfessionSessionDto;
import org.serviceproject.confession.dto.CreateConfessionRequest;
import org.serviceproject.confession.dto.CreateConfessionSessionRequest;
import org.serviceproject.confession.dto.UpdateConfessionRequest;
import org.serviceproject.confession.service.ConfessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for student confession records.
 */
@RestController
@RequestMapping("/api/confessions")
@RequiredArgsConstructor
public class ConfessionController {

    private final ConfessionService confessionService;

    @GetMapping("/sessions")
    public ResponseEntity<List<ConfessionSessionDto>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(confessionService.getSessions(principal));
    }

    @PostMapping("/session")
    public ResponseEntity<ConfessionSessionDto> createSession(
            @Valid @RequestBody CreateConfessionSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConfessionSessionDto response = confessionService.createSession(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/overview")
    public ResponseEntity<List<org.serviceproject.confession.dto.StudentConfessionSummaryDto>> getOverview(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(confessionService.getOverview(principal));
    }

    @GetMapping
    public ResponseEntity<List<ConfessionResponse>> findByStudent(
            @RequestParam Long studentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(confessionService.findByStudent(studentId, principal));
    }

    @PostMapping
    public ResponseEntity<ConfessionResponse> create(
            @Valid @RequestBody CreateConfessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConfessionResponse response = confessionService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfessionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConfessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(confessionService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        confessionService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
