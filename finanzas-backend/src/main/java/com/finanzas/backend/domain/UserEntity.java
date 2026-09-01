package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellido = "";

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 512)
    private String passwordHash;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(nullable = false, length = 20)
    private String locale = "es-CO";

    @Column(name = "tipo_cuenta", nullable = false, length = 40)
    private String tipoCuenta = "PERSONAL";

    @Column(name = "avatar_ref", length = 1024)
    private String avatarRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_autenticacion", nullable = false, length = 40)
    private AuthProvider authProvider = AuthProvider.PASSWORD;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String nombre, String apellido, String email, String passwordHash, String moneda, String tipoCuenta, AuthProvider authProvider) {
        this.nombre = required(nombre, "nombre");
        this.apellido = apellido == null ? "" : apellido.trim();
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.moneda = moneda == null || moneda.trim().isEmpty() ? "COP" : moneda.trim().toUpperCase(Locale.ROOT);
        this.tipoCuenta = tipoCuenta == null || tipoCuenta.trim().isEmpty() ? "PERSONAL" : tipoCuenta.trim().toUpperCase(Locale.ROOT);
        this.authProvider = authProvider == null ? AuthProvider.PASSWORD : authProvider;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getMoneda() { return moneda; }
    public String getLocale() { return locale; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getAvatarRef() { return avatarRef; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateProfile(String nombre, String apellido, String email, String moneda, String locale) {
        this.nombre = required(nombre, "nombre");
        this.apellido = apellido == null ? "" : apellido.trim();
        this.email = normalizeEmail(email);
        if (moneda != null && !moneda.trim().isEmpty()) {
            this.moneda = moneda.trim().toUpperCase(Locale.ROOT);
        }
        if (locale != null && !locale.trim().isEmpty()) {
            this.locale = locale.trim();
        }
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setAvatarRef(String avatarRef) {
        this.avatarRef = avatarRef;
    }

    public void linkGoogle() {
        this.authProvider = AuthProvider.GOOGLE;
    }

    public static String normalizeEmail(String email) {
        return required(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
