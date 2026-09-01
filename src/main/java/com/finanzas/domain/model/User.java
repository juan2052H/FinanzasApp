package com.finanzas.domain.model;

import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class User {
    private final UUID id;
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash;
    private Currency moneda;
    private Locale locale;
    private String tipoCuenta;
    private String avatar;
    private AuthProvider proveedorAutenticacion;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, String nombre, String apellido, String email, String passwordHash,
                Currency moneda, Locale locale, String tipoCuenta, String avatar,
                AuthProvider proveedorAutenticacion, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = safe(nombre);
        this.apellido = safe(apellido);
        setEmail(email);
        this.passwordHash = safe(passwordHash);
        this.moneda = moneda == null ? Currency.getInstance("COP") : moneda;
        this.locale = locale == null ? new Locale("es", "CO") : locale;
        this.tipoCuenta = safe(tipoCuenta);
        this.avatar = safe(avatar);
        this.proveedorAutenticacion = proveedorAutenticacion == null ? AuthProvider.PASSWORD : proveedorAutenticacion;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static User passwordUser(String nombre, String apellido, String email, String passwordHash) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), nombre, apellido, email, passwordHash, Currency.getInstance("COP"),
                new Locale("es", "CO"), "Personal", "", AuthProvider.PASSWORD, now, now);
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Currency getMoneda() { return moneda; }
    public Locale getLocale() { return locale; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getAvatar() { return avatar; }
    public AuthProvider getProveedorAutenticacion() { return proveedorAutenticacion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void rename(String nombre, String apellido) {
        this.nombre = safe(nombre);
        this.apellido = safe(apellido);
        touch();
    }

    public void setEmail(String email) {
        String normalized = safe(email).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Correo electronico invalido.");
        }
        this.email = normalized;
        touch();
    }

    public void setAvatar(String avatar) {
        this.avatar = safe(avatar);
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
