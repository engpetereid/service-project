package org.serviceproject.visits.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.visits.dto.ServantCurrentWeekResponse;
import org.serviceproject.visits.dto.UpdateVisitRequest;
import org.serviceproject.visits.dto.VisitRequest;
import org.serviceproject.visits.dto.VisitResponse;
import org.serviceproject.visits.service.VisitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * REST controller for weekly follow-up and visits.
 */
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping("/current-week")
    public ResponseEntity<ServantCurrentWeekResponse> getCurrentWeekWorkflow(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(visitService.getCurrentWeekWorkflow(principal));
    }

    @GetMapping
    public ResponseEntity<List<VisitResponse>> findByWeek(
            @RequestParam Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(visitService.findByWeek(weekId, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(visitService.findById(id, principal));
    }

    @PostMapping
    public ResponseEntity<VisitResponse> create(
            @Valid @RequestBody VisitRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VisitResponse response = visitService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVisitRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(visitService.update(id, request, principal));
    }
}
