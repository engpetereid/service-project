package org.serviceproject.students.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.students.dto.BatchAssignServantRequest;
import org.serviceproject.students.dto.BatchMoveClassRequest;
import org.serviceproject.students.dto.ChangeAssignmentRequest;
import org.serviceproject.students.dto.CreateStudentRequest;
import org.serviceproject.students.dto.StudentResponse;
import org.serviceproject.students.dto.UpdateStudentRequest;
import org.serviceproject.students.service.StudentService;
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
 * REST controller for managing students and student placements.
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<List<StudentResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ministryId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long servantId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(studentService.findAll(principal, ministryId, classId, servantId, scope, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentService.findById(id, principal));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateStudentRequest request) {
        StudentResponse response = studentService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request, principal));
    }

    @PutMapping("/{id}/assignment")
    public ResponseEntity<StudentResponse> changeAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChangeAssignmentRequest request) {
        return ResponseEntity.ok(studentService.changeAssignment(id, request, principal));
    }

    @PutMapping("/batch/servant")
    public ResponseEntity<List<StudentResponse>> batchAssignServant(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BatchAssignServantRequest request) {
        return ResponseEntity.ok(studentService.batchAssignServant(request, principal));
    }

    @PutMapping("/batch/class")
    public ResponseEntity<List<StudentResponse>> batchMoveClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BatchMoveClassRequest request) {
        return ResponseEntity.ok(studentService.batchMoveClass(request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        studentService.softDelete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        studentService.restore(id);
        return ResponseEntity.ok().build();
    }
}
