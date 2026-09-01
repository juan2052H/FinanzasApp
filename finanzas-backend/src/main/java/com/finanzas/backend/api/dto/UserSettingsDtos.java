package com.finanzas.backend.api.dto;

public final class UserSettingsDtos {
    private UserSettingsDtos() {
    }

    public record UserSettingsResponse(
            String theme,
            String locale,
            String timeZone,
            String moneyFormat,
            boolean notifPresupuesto,
            boolean notifMetas,
            boolean notifConsejos,
            long version) {
    }

    public record UserSettingsPatchRequest(
            String theme,
            String locale,
            String timeZone,
            String moneyFormat,
            Boolean notifPresupuesto,
            Boolean notifMetas,
            Boolean notifConsejos) {
    }
}
