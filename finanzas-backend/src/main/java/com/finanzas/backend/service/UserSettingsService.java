package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.UserSettingsDtos;
import com.finanzas.backend.domain.MoneyFormat;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.UserSettingsEntity;
import com.finanzas.backend.domain.UserTheme;
import com.finanzas.backend.repo.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class UserSettingsService {
    private final UserSettingsRepository settings;

    public UserSettingsService(UserSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional
    public UserSettingsDtos.UserSettingsResponse get(UserEntity user) {
        return toResponse(settingsFor(user));
    }

    @Transactional
    public UserSettingsDtos.UserSettingsResponse update(UserEntity user, UserSettingsDtos.UserSettingsPatchRequest request) {
        UserSettingsEntity entity = settingsFor(user);
        if (request.theme() != null) {
            entity.setTheme(parseEnum(UserTheme.class, request.theme(), "theme"));
        }
        if (request.locale() != null) {
            String locale = normalizeLocale(request.locale());
            entity.setLocale(locale);
            user.setLocale(locale);
        }
        if (request.timeZone() != null) {
            entity.setTimeZone(normalizeTimeZone(request.timeZone()));
        }
        if (request.moneyFormat() != null) {
            entity.setMoneyFormat(parseEnum(MoneyFormat.class, request.moneyFormat(), "moneyFormat"));
        }
        if (request.notifPresupuesto() != null) {
            entity.setNotifPresupuesto(request.notifPresupuesto());
        }
        if (request.notifMetas() != null) {
            entity.setNotifMetas(request.notifMetas());
        }
        if (request.notifConsejos() != null) {
            entity.setNotifConsejos(request.notifConsejos());
        }
        return toResponse(entity);
    }

    private UserSettingsEntity settingsFor(UserEntity user) {
        return settings.findById(user.getId())
                .orElseGet(() -> settings.save(new UserSettingsEntity(user.getId(), user.getLocale())));
    }

    private String normalizeLocale(String raw) {
        String value = raw == null ? "" : raw.trim().replace('_', '-');
        Locale locale = Locale.forLanguageTag(value);
        if (value.isEmpty() || locale.getLanguage().isEmpty()) {
            throw invalid("locale");
        }
        return locale.toLanguageTag();
    }

    private String normalizeTimeZone(String raw) {
        String value = raw == null ? "" : raw.trim();
        try {
            if (value.isEmpty()) {
                throw invalid("timeZone");
            }
            return ZoneId.of(value).getId();
        } catch (DateTimeException ex) {
            throw invalid("timeZone");
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw, String field) {
        try {
            return Enum.valueOf(type, raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalid(field);
        }
    }

    private ResponseStatusException invalid(String field) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Preferencia invalida: " + field + ".");
    }

    private UserSettingsDtos.UserSettingsResponse toResponse(UserSettingsEntity entity) {
        return new UserSettingsDtos.UserSettingsResponse(
                entity.getTheme().name(),
                entity.getLocale(),
                entity.getTimeZone(),
                entity.getMoneyFormat().name(),
                entity.isNotifPresupuesto(),
                entity.isNotifMetas(),
                entity.isNotifConsejos(),
                entity.getVersion());
    }
}
