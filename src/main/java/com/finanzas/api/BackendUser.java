package com.finanzas.api;

public final class BackendUser {
    private final String id;
    private final String nombre;
    private final String apellido;
    private final String email;
    private final String ciudad;
    private final String pais;
    private final String moneda;
    private final String locale;
    private final String tipoCuenta;
    private final String avatarRef;
    private final boolean emailVerified;

    public BackendUser(String id, String nombre, String apellido, String email, String moneda, String locale, String tipoCuenta, String avatarRef) {
        this(id, nombre, apellido, email, "", "", moneda, locale, tipoCuenta, avatarRef, false);
    }

    public BackendUser(String id, String nombre, String apellido, String email, String ciudad, String pais, String moneda, String locale,
                       String tipoCuenta, String avatarRef, boolean emailVerified) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.apellido = apellido == null ? "" : apellido;
        this.email = email == null ? "" : email;
        this.ciudad = ciudad == null ? "" : ciudad;
        this.pais = pais == null ? "" : pais;
        this.moneda = moneda == null || moneda.trim().isEmpty() ? "COP" : moneda;
        this.locale = locale == null ? "es-CO" : locale;
        this.tipoCuenta = tipoCuenta == null || tipoCuenta.trim().isEmpty() ? "PERSONAL" : tipoCuenta;
        this.avatarRef = avatarRef == null ? "" : avatarRef;
        this.emailVerified = emailVerified;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getCiudad() { return ciudad; }
    public String getPais() { return pais; }
    public String getMoneda() { return moneda; }
    public String getLocale() { return locale; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getAvatarRef() { return avatarRef; }
    public boolean isEmailVerified() { return emailVerified; }
}
