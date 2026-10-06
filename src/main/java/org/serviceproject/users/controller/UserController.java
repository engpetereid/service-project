package org.serviceproject.users.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.users.dto.AssignRolesRequest;
import org.serviceproject.users.dto.CreateUserRequest;
import org.serviceproject.users.dto.UpdateUserRequest;
import org.serviceproject.users.dto.UserResponse;
import org.serviceproject.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.serviceproject.common.security.UserPrincipal;
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
 * User management endpoints (admin only).
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('GENERAL_ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRoles(id, request));
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        userService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        userService.restoreDeleted(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/from-person/{personId}")
    public ResponseEntity<UserResponse> createForPerson(
            @PathVariable Long personId,
            @jakarta.validation.Valid @RequestBody org.serviceproject.users.dto.CreateUserForPersonRequest request) {
        String pass = request.password();
        var roles = request.roles();
        UserResponse response = userService.createForPerson(personId, pass, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody org.serviceproject.users.dto.ResetPasswordRequest request) {
        String pass = request.newPassword();
        userService.resetPassword(id, pass);
        return ResponseEntity.ok().build();
    }
}
