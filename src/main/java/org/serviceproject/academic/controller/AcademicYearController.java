package org.serviceproject.academic.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.dto.AcademicYearResponse;
import org.serviceproject.academic.service.AcademicYearService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Academic year endpoints. Read-only — years are auto-managed.
 */
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    public ResponseEntity<List<AcademicYearResponse>> findAll() {
        return ResponseEntity.ok(academicYearService.findAll());
    }

    @GetMapping("/current")
    public ResponseEntity<AcademicYearResponse> findCurrent() {
        return ResponseEntity.ok(academicYearService.findCurrent());
    }
}
