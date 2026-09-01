package com.finanzas.api;

public final class BackendUser {
    private final String id;
    private final String nombre;
    private final String apellido;
    private final String email;
    private final String moneda;
    private final String locale;
    private final String tipoCuenta;
    private final String avatarRef;

    public BackendUser(String id, String nombre, String apellido, String email, String moneda, String locale, String tipoCuenta, String avatarRef) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.apellido = apellido == null ? "" : apellido;
        this.email = email == null ? "" : email;
        this.moneda = moneda == null || moneda.trim().isEmpty() ? "COP" : moneda;
        this.locale = locale == null ? "es-CO" : locale;
        this.tipoCuenta = tipoCuenta == null || tipoCuenta.trim().isEmpty() ? "PERSONAL" : tipoCuenta;
        this.avatarRef = avatarRef == null ? "" : avatarRef;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getMoneda() { return moneda; }
    public String getLocale() { return locale; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getAvatarRef() { return avatarRef; }
}
