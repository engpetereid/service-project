package org.serviceproject.settings.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.settings.dto.PublicSettingsResponse;
import org.serviceproject.settings.dto.SettingResponse;
import org.serviceproject.settings.dto.UpdateSettingRequest;
import org.serviceproject.settings.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for application system settings.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Lists all system settings (General Admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<List<SettingResponse>> getAllSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SettingResponse> response = settingsService.getAllSettings(principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single system setting by key (General Admin only).
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<SettingResponse> getSettingByKey(
            @PathVariable String key,
            @AuthenticationPrincipal UserPrincipal principal) {
        SettingResponse response = settingsService.getSettingByKey(key, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a system setting (General Admin only).
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<SettingResponse> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SettingResponse response = settingsService.updateSetting(key, request.value(), principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves public settings accessible to all authenticated users.
     */
    @GetMapping("/public")
    public ResponseEntity<PublicSettingsResponse> getPublicSettings() {
        PublicSettingsResponse response = settingsService.getPublicSettings();
        return ResponseEntity.ok(response);
    }
}
