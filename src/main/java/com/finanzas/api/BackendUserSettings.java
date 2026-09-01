package com.finanzas.api;

public final class BackendUserSettings {
    private final String theme;
    private final String locale;
    private final String timeZone;
    private final String moneyFormat;
    private final boolean notifPresupuesto;
    private final boolean notifMetas;
    private final boolean notifConsejos;
    private final long version;

    public BackendUserSettings(String theme, String locale, String timeZone, String moneyFormat,
                               boolean notifPresupuesto, boolean notifMetas, boolean notifConsejos, long version) {
        this.theme = theme == null || theme.trim().isEmpty() ? "LIGHT" : theme.trim();
        this.locale = locale == null || locale.trim().isEmpty() ? "es-CO" : locale.trim();
        this.timeZone = timeZone == null || timeZone.trim().isEmpty() ? "America/Bogota" : timeZone.trim();
        this.moneyFormat = moneyFormat == null || moneyFormat.trim().isEmpty() ? "SYMBOL_GROUP_DECIMAL" : moneyFormat.trim();
        this.notifPresupuesto = notifPresupuesto;
        this.notifMetas = notifMetas;
        this.notifConsejos = notifConsejos;
        this.version = version;
    }

    public String getTheme() { return theme; }
    public String getLocale() { return locale; }
    public String getTimeZone() { return timeZone; }
    public String getMoneyFormat() { return moneyFormat; }
    public boolean isNotifPresupuesto() { return notifPresupuesto; }
    public boolean isNotifMetas() { return notifMetas; }
    public boolean isNotifConsejos() { return notifConsejos; }
    public long getVersion() { return version; }
}
