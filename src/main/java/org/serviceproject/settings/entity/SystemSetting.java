package org.serviceproject.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

/**
 * Key-value application setting stored in the database.
 * <p>
 * General Admin can modify settings via the Settings page.
 * The application reads configured values dynamically (e.g. MAX_NOTE_SCORE).
 */
@Entity
@Table(name = "system_setting")
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    public SystemSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }
}
