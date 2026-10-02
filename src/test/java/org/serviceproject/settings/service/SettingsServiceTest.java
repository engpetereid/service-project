package org.serviceproject.settings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.settings.dto.PublicSettingsResponse;
import org.serviceproject.settings.dto.SettingResponse;
import org.serviceproject.settings.entity.SystemSetting;
import org.serviceproject.settings.repository.SystemSettingRepository;
import org.serviceproject.users.entity.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SettingsService settingsService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal servantPrincipal;
    private SystemSetting setting;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        servantPrincipal = new UserPrincipal(2L, 20L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        setting = new SystemSetting("MAX_NOTE_SCORE", "21");
        setting.setId(100L);
    }

    @Test
    void getMaxNoteScore_exists_returnsParsedInt() {
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.of(setting));

        int max = settingsService.getMaxNoteScore();

        assertEquals(21, max);
    }

    @Test
    void getMaxNoteScore_notExists_returnsDefault21() {
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.empty());

        int max = settingsService.getMaxNoteScore();

        assertEquals(21, max);
    }

    @Test
    void getMaxNoteScore_invalidInt_returnsDefault21() {
        SystemSetting invalid = new SystemSetting("MAX_NOTE_SCORE", "invalid_num");
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.of(invalid));

        int max = settingsService.getMaxNoteScore();

        assertEquals(21, max);
    }

    @Test
    void getAllSettings_admin_returnsList() {
        when(systemSettingRepository.findAll()).thenReturn(List.of(setting));

        List<SettingResponse> results = settingsService.getAllSettings(adminPrincipal);

        assertEquals(1, results.size());
        assertEquals("MAX_NOTE_SCORE", results.get(0).key());
    }

    @Test
    void getAllSettings_nonAdmin_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                settingsService.getAllSettings(servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getSettingByKey_admin_returnsSetting() {
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.of(setting));

        SettingResponse result = settingsService.getSettingByKey("MAX_NOTE_SCORE", adminPrincipal);

        assertEquals("MAX_NOTE_SCORE", result.key());
        assertEquals("21", result.value());
    }

    @Test
    void updateSetting_admin_validScore_updatesAndDispatchesAudit() {
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.of(setting));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingResponse result = settingsService.updateSetting("MAX_NOTE_SCORE", "25", adminPrincipal);

        assertEquals("25", result.value());
        assertEquals("25", setting.getSettingValue());
        verify(eventPublisher).publishEvent(any(AuditEvent.class));
    }

    @Test
    void updateSetting_admin_invalidScore_throwsBadRequest() {
        AppException ex = assertThrows(AppException.class, () ->
                settingsService.updateSetting("MAX_NOTE_SCORE", "-5", adminPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_SETTING_VALUE", ex.getCode());
    }

    @Test
    void updateSetting_nonAdmin_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                settingsService.updateSetting("MAX_NOTE_SCORE", "25", servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getPublicSettings_returnsMaxNoteScore() {
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")).thenReturn(Optional.of(setting));

        PublicSettingsResponse response = settingsService.getPublicSettings();

        assertEquals(21, response.maxNoteScore());
    }
}
