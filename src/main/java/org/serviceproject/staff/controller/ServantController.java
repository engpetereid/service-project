package org.serviceproject.staff.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.staff.dto.CreateServantRequest;
import org.serviceproject.staff.dto.ServantResponse;
import org.serviceproject.staff.dto.UpdateServantRequest;
import org.serviceproject.staff.service.ServantService;
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
 * Controller managing servants and their placements.
 * Scope filtering is enforced inside {@link ServantService}.
 */
@RestController
@RequestMapping("/api/servants")
@RequiredArgsConstructor
public class ServantController {

    private final ServantService servantService;

    @GetMapping
    public ResponseEntity<List<ServantResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ministryId,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(servantService.findAll(principal, ministryId, classId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServantResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(servantService.findById(id, principal));
    }

    @PostMapping
    public ResponseEntity<ServantResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateServantRequest request) {
        ServantResponse response = servantService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServantResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateServantRequest request) {
        return ResponseEntity.ok(servantService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        servantService.softDelete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> restore(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        servantService.restore(id, principal);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/account")
    public ResponseEntity<ServantResponse> createAccount(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String password = body != null ? body.get("password") : null;
        return ResponseEntity.ok(servantService.createOrEnableAccount(id, password, principal));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ServantResponse> resetPassword(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String password = body != null ? (body.containsKey("newPassword") ? body.get("newPassword") : body.get("password")) : null;
        return ResponseEntity.ok(servantService.createOrEnableAccount(id, password, principal));
    }
}
