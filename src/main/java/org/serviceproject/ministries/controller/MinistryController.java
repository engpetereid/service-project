package org.serviceproject.ministries.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.dto.AssignSecretaryRequest;
import org.serviceproject.ministries.dto.MinistryRequest;
import org.serviceproject.ministries.dto.MinistryResponse;
import org.serviceproject.ministries.service.MinistryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ministry management endpoints. Write operations are admin-only.
 * Read operations are available to all authenticated users.
 */
@RestController
@RequestMapping("/api/ministries")
@RequiredArgsConstructor
public class MinistryController {

    private final MinistryService ministryService;

    @GetMapping
    public ResponseEntity<List<MinistryResponse>> findAll() {
        return ResponseEntity.ok(ministryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MinistryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ministryService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<MinistryResponse> create(
            @Valid @RequestBody MinistryRequest request) {
        MinistryResponse response = ministryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<MinistryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MinistryRequest request) {
        return ResponseEntity.ok(ministryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        ministryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/secretary")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> assignSecretary(
            @PathVariable Long id,
            @RequestBody AssignSecretaryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ministryService.assignSecretary(id, request, principal);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/secretary")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> removeSecretary(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ministryService.removeSecretary(id, principal);
        return ResponseEntity.noContent().build();
    }
}
