package com.finanzas.model;

import java.io.Serializable;

public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private String apellido;
    private String ciudad;
    private String pais;
    private String email;
    private boolean notifPresupuesto;
    private boolean notifMetas;
    private boolean notifConsejos;
    private String moneda;
    private String locale;
    private String timeZone;
    private String moneyFormat;
    private String theme;
    private String tipoCuenta;
    private String profileImagePath;

    public Usuario() {
        nombre = "";
        apellido = "";
        ciudad = "";
        pais = "";
        email = "";
        notifPresupuesto = true;
        notifMetas = true;
        notifConsejos = true;
        moneda = "COP";
        locale = "es-CO";
        timeZone = "America/Bogota";
        moneyFormat = "SYMBOL_GROUP_DECIMAL";
        theme = "LIGHT";
        tipoCuenta = "Personal";
        profileImagePath = "";
    }

    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getNombreCompleto() { return (nombre + " " + apellido).trim(); }
    public String getCiudad() { return ciudad; }
    public String getPais() { return pais; }
    public String getUbicacion() {
        if ((ciudad == null || ciudad.trim().isEmpty()) && (pais == null || pais.trim().isEmpty())) {
            return "";
        }
        if (ciudad == null || ciudad.trim().isEmpty()) {
            return pais;
        }
        if (pais == null || pais.trim().isEmpty()) {
            return ciudad;
        }
        return ciudad + ", " + pais;
    }
    public String getEmail() { return email; }
    public boolean isNotifPresupuesto() { return notifPresupuesto; }
    public boolean isNotifMetas() { return notifMetas; }
    public boolean isNotifConsejos() { return notifConsejos; }
    public String getMoneda() { return moneda; }
    public String getLocale() { return locale == null || locale.trim().isEmpty() ? "es-CO" : locale; }
    public String getTimeZone() { return timeZone == null || timeZone.trim().isEmpty() ? "America/Bogota" : timeZone; }
    public String getMoneyFormat() { return moneyFormat == null || moneyFormat.trim().isEmpty() ? "SYMBOL_GROUP_DECIMAL" : moneyFormat; }
    public String getTheme() { return theme == null || theme.trim().isEmpty() ? "LIGHT" : theme; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getProfileImagePath() { return profileImagePath; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public void setPais(String pais) { this.pais = pais; }
    public void setEmail(String email) { this.email = email; }
    public void setNotifPresupuesto(boolean notifPresupuesto) { this.notifPresupuesto = notifPresupuesto; }
    public void setNotifMetas(boolean notifMetas) { this.notifMetas = notifMetas; }
    public void setNotifConsejos(boolean notifConsejos) { this.notifConsejos = notifConsejos; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public void setLocale(String locale) { this.locale = locale == null || locale.trim().isEmpty() ? "es-CO" : locale.trim(); }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone == null || timeZone.trim().isEmpty() ? "America/Bogota" : timeZone.trim(); }
    public void setMoneyFormat(String moneyFormat) { this.moneyFormat = moneyFormat == null || moneyFormat.trim().isEmpty() ? "SYMBOL_GROUP_DECIMAL" : moneyFormat.trim(); }
    public void setTheme(String theme) { this.theme = theme == null || theme.trim().isEmpty() ? "LIGHT" : theme.trim(); }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath == null ? "" : profileImagePath; }
}
