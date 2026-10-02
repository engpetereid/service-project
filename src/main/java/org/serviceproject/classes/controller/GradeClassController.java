package org.serviceproject.classes.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.classes.dto.GradeClassRequest;
import org.serviceproject.classes.dto.GradeClassResponse;
import org.serviceproject.classes.service.GradeClassService;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.dto.AssignSecretaryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Class management endpoints. Write operations available to General Admin
 * and Service Secretary (scoped to own ministry).
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class GradeClassController {

    private final GradeClassService gradeClassService;

    @GetMapping
    public ResponseEntity<List<GradeClassResponse>> findAll(
            @RequestParam(required = false) Long ministryId) {
        List<GradeClassResponse> result = ministryId != null
                ? gradeClassService.findByMinistry(ministryId)
                : gradeClassService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradeClassResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(gradeClassService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GENERAL_ADMIN', 'SERVICE_SECRETARY')")
    public ResponseEntity<GradeClassResponse> create(
            @Valid @RequestBody GradeClassRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        GradeClassResponse response = gradeClassService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_ADMIN', 'SERVICE_SECRETARY')")
    public ResponseEntity<GradeClassResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GradeClassRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gradeClassService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_ADMIN', 'SERVICE_SECRETARY')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        gradeClassService.deactivate(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/secretary")
    @PreAuthorize("hasAnyRole('GENERAL_ADMIN', 'SERVICE_SECRETARY')")
    public ResponseEntity<Void> assignSecretary(
            @PathVariable Long id,
            @RequestBody AssignSecretaryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        gradeClassService.assignSecretary(id, request, principal);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/secretary")
    @PreAuthorize("hasAnyRole('GENERAL_ADMIN', 'SERVICE_SECRETARY')")
    public ResponseEntity<Void> removeSecretary(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        gradeClassService.removeSecretary(id, principal);
        return ResponseEntity.noContent().build();
    }
}
