package org.serviceproject.weeks.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.weeks.dto.WeekRequest;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST controller for week retrieval and admin management.
 */
@RestController
@RequestMapping("/api/weeks")
@RequiredArgsConstructor
public class WeekController {

    private final WeekService weekService;

    @GetMapping("/current")
    public ResponseEntity<WeekResponse> getCurrentWeek() {
        return ResponseEntity.ok(weekService.getCurrentWeek());
    }

    @GetMapping
    public ResponseEntity<List<WeekResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(weekService.findAll(includeDeleted));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeekResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(weekService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<WeekResponse> create(@Valid @RequestBody WeekRequest request) {
        WeekResponse response = weekService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<WeekResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody WeekRequest request) {
        return ResponseEntity.ok(weekService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        weekService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        weekService.restore(id);
        return ResponseEntity.ok().build();
    }
}
