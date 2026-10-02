package org.serviceproject.academic.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.dto.PromotionRunResponse;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.academic.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for executing and monitoring promotion runs. Admin only.
 */
@RestController
@RequestMapping("/api/promotion")
@PreAuthorize("hasRole('GENERAL_ADMIN')")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final AcademicYearService academicYearService;

    @GetMapping("/history")
    public ResponseEntity<List<PromotionRunResponse>> getHistory() {
        return ResponseEntity.ok(promotionService.getHistory());
    }

    @GetMapping("/year/{academicYearId}")
    public ResponseEntity<PromotionRunResponse> getRunForYear(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(promotionService.getRunForYear(academicYearId));
    }

    @PostMapping("/run")
    public ResponseEntity<PromotionRunResponse> runPromotion(
            @RequestParam(required = false) Long targetYearId) {
        Long yearId = targetYearId != null ? targetYearId : academicYearService.findCurrent().id();
        return ResponseEntity.ok(promotionService.executePromotion(yearId));
    }
}
