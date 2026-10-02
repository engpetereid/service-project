package org.serviceproject.settings.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.settings.dto.PublicSettingsResponse;
import org.serviceproject.settings.dto.SettingResponse;
import org.serviceproject.settings.dto.UpdateSettingRequest;
import org.serviceproject.settings.service.SettingsService;
import org.serviceproject.users.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private SettingsController settingsController;

    private UserPrincipal adminPrincipal;
    private SettingResponse settingResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        settingResponse = new SettingResponse(1L, "MAX_NOTE_SCORE", "21", LocalDateTime.now());
    }

    @Test
    void getAllSettings_returnsOkWithList() {
        when(settingsService.getAllSettings(adminPrincipal)).thenReturn(List.of(settingResponse));

        ResponseEntity<List<SettingResponse>> response = settingsController.getAllSettings(adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("MAX_NOTE_SCORE", response.getBody().get(0).key());
    }

    @Test
    void getSettingByKey_returnsOkWithSetting() {
        when(settingsService.getSettingByKey("MAX_NOTE_SCORE", adminPrincipal)).thenReturn(settingResponse);

        ResponseEntity<SettingResponse> response = settingsController.getSettingByKey("MAX_NOTE_SCORE", adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("21", response.getBody().value());
    }

    @Test
    void updateSetting_returnsOkWithUpdatedSetting() {
        SettingResponse updated = new SettingResponse(1L, "MAX_NOTE_SCORE", "25", LocalDateTime.now());
        when(settingsService.updateSetting("MAX_NOTE_SCORE", "25", adminPrincipal)).thenReturn(updated);

        UpdateSettingRequest request = new UpdateSettingRequest("25");
        ResponseEntity<SettingResponse> response = settingsController.updateSetting("MAX_NOTE_SCORE", request, adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("25", response.getBody().value());
    }

    @Test
    void getPublicSettings_returnsOkWithPublicSettings() {
        PublicSettingsResponse publicSettings = new PublicSettingsResponse(21);
        when(settingsService.getPublicSettings()).thenReturn(publicSettings);

        ResponseEntity<PublicSettingsResponse> response = settingsController.getPublicSettings();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(21, response.getBody().maxNoteScore());
    }
}
