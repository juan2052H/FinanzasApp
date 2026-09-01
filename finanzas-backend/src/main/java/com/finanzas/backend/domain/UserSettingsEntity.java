package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserTheme theme = UserTheme.LIGHT;

    @Column(nullable = false, length = 20)
    private String locale = "es-CO";

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone = "America/Bogota";

    @Enumerated(EnumType.STRING)
    @Column(name = "money_format", nullable = false, length = 40)
    private MoneyFormat moneyFormat = MoneyFormat.SYMBOL_GROUP_DECIMAL;

    @Column(name = "notif_presupuesto", nullable = false)
    private boolean notifPresupuesto = true;

    @Column(name = "notif_metas", nullable = false)
    private boolean notifMetas = true;

    @Column(name = "notif_consejos", nullable = false)
    private boolean notifConsejos = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSettingsEntity() {
    }

    public UserSettingsEntity(UUID userId, String locale) {
        this.userId = userId;
        if (locale != null && !locale.trim().isEmpty()) {
            this.locale = locale.trim();
        }
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public UserTheme getTheme() { return theme; }
    public String getLocale() { return locale; }
    public String getTimeZone() { return timeZone; }
    public MoneyFormat getMoneyFormat() { return moneyFormat; }
    public boolean isNotifPresupuesto() { return notifPresupuesto; }
    public boolean isNotifMetas() { return notifMetas; }
    public boolean isNotifConsejos() { return notifConsejos; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTheme(UserTheme theme) { this.theme = theme == null ? UserTheme.LIGHT : theme; }
    public void setLocale(String locale) { this.locale = locale == null || locale.trim().isEmpty() ? "es-CO" : locale.trim(); }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone == null || timeZone.trim().isEmpty() ? "America/Bogota" : timeZone.trim(); }
    public void setMoneyFormat(MoneyFormat moneyFormat) { this.moneyFormat = moneyFormat == null ? MoneyFormat.SYMBOL_GROUP_DECIMAL : moneyFormat; }
    public void setNotifPresupuesto(boolean notifPresupuesto) { this.notifPresupuesto = notifPresupuesto; }
    public void setNotifMetas(boolean notifMetas) { this.notifMetas = notifMetas; }
    public void setNotifConsejos(boolean notifConsejos) { this.notifConsejos = notifConsejos; }
}
