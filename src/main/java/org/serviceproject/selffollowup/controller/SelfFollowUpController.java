package org.serviceproject.selffollowup.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.selffollowup.dto.SelfFollowUpCurrentWeekResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpRequest;
import org.serviceproject.selffollowup.dto.SelfFollowUpResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpStatsResponse;
import org.serviceproject.selffollowup.service.SelfFollowUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for personal weekly self-follow-up (متابعتي الأسبوعية).
 */
@RestController
@RequestMapping("/api/self-followup")
@RequiredArgsConstructor
public class SelfFollowUpController {

    private final SelfFollowUpService selfFollowUpService;

    @GetMapping("/current-week")
    public ResponseEntity<SelfFollowUpCurrentWeekResponse> getCurrentWeek(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(selfFollowUpService.getCurrentWeek(principal));
    }

    @PutMapping
    public ResponseEntity<SelfFollowUpResponse> upsert(
            @Valid @RequestBody SelfFollowUpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(selfFollowUpService.upsert(request, principal));
    }

    @GetMapping("/weeks/{weekId}")
    public ResponseEntity<SelfFollowUpResponse> getByWeek(
            @PathVariable Long weekId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(selfFollowUpService.getByWeek(weekId, principal));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SelfFollowUpResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(selfFollowUpService.getHistory(principal));
    }

    @GetMapping("/statistics")
    public ResponseEntity<SelfFollowUpStatsResponse> getStatistics(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(selfFollowUpService.getStatistics(principal));
    }
}
