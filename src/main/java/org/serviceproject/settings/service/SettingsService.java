package org.serviceproject.settings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.settings.dto.PublicSettingsResponse;
import org.serviceproject.settings.dto.SettingResponse;
import org.serviceproject.settings.entity.SystemSetting;
import org.serviceproject.settings.repository.SystemSettingRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service managing system-wide settings dynamically stored in the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    public static final String KEY_MAX_NOTE_SCORE = "MAX_NOTE_SCORE";
    public static final int DEFAULT_MAX_NOTE_SCORE = 21;

    private final SystemSettingRepository systemSettingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Dynamically resolves the maximum note score (default: 21).
     */
    @Transactional(readOnly = true)
    public int getMaxNoteScore() {
        return systemSettingRepository.findBySettingKey(KEY_MAX_NOTE_SCORE)
                .map(SystemSetting::getSettingValue)
                .map(val -> {
                    try {
                        return Integer.parseInt(val.trim());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid integer in MAX_NOTE_SCORE setting: '{}', falling back to default {}", val, DEFAULT_MAX_NOTE_SCORE);
                        return DEFAULT_MAX_NOTE_SCORE;
                    }
                })
                .orElse(DEFAULT_MAX_NOTE_SCORE);
    }

    /**
     * Lists all application settings (General Admin only).
     */
    @Transactional(readOnly = true)
    public List<SettingResponse> getAllSettings(UserPrincipal principal) {
        validateAdmin(principal);
        return systemSettingRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single setting by key.
     */
    @Transactional(readOnly = true)
    public SettingResponse getSettingByKey(String key, UserPrincipal principal) {
        validateAdmin(principal);
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> AppException.notFound("SETTING_NOT_FOUND", "الإعداد غير موجود"));
        return toResponse(setting);
    }

    /**
     * Updates an application setting and dispatches an audit event (General Admin only).
     */
    @Transactional
    public SettingResponse updateSetting(String key, String value, UserPrincipal principal) {
        validateAdmin(principal);

        if (value == null || value.trim().isEmpty()) {
            throw AppException.badRequest("INVALID_SETTING_VALUE", "قيمة الإعداد لا يمكن أن تكون فارغة");
        }
        String trimmedValue = value.trim();

        if (KEY_MAX_NOTE_SCORE.equalsIgnoreCase(key)) {
            try {
                int parsed = Integer.parseInt(trimmedValue);
                if (parsed <= 0 || parsed > 1000) {
                    throw AppException.badRequest("INVALID_SETTING_VALUE", "الحد الأقصى للدرجة يجب أن يكون رقمًا صحيحًا موجبًا");
                }
            } catch (NumberFormatException e) {
                throw AppException.badRequest("INVALID_SETTING_VALUE", "الحد الأقصى للدرجة يجب أن يكون رقمًا صحيحًا");
            }
        }

        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> new SystemSetting(key, trimmedValue));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(trimmedValue);
        setting = systemSettingRepository.save(setting);

        log.info("System setting '{}' updated from '{}' to '{}' by admin userId {}",
                key, oldValue, trimmedValue, principal.getUserId());

        eventPublisher.publishEvent(AuditEvent.of(
                principal,
                AuditAction.SETTING_CHANGE,
                "SystemSetting",
                setting.getId(),
                Map.of("key", key, "value", oldValue != null ? oldValue : ""),
                Map.of("key", key, "value", trimmedValue)
        ));

        return toResponse(setting);
    }

    /**
     * Retrieves public settings accessible to all authenticated users.
     */
    @Transactional(readOnly = true)
    public PublicSettingsResponse getPublicSettings() {
        return new PublicSettingsResponse(getMaxNoteScore());
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private void validateAdmin(UserPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw AppException.forbidden("ACCESS_DENIED", "غير مصرح لك بإدارة الإعدادات");
        }
    }

    private SettingResponse toResponse(SystemSetting s) {
        return new SettingResponse(
                s.getId(),
                s.getSettingKey(),
                s.getSettingValue(),
                s.getUpdatedAt()
        );
    }
}
