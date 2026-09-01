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
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private CategoryType type;

    @Column(nullable = false, length = 80)
    private String icono = "";

    @Column(nullable = false, length = 16)
    private String color = "#1a73e8";

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryEntity() {
    }

    public CategoryEntity(UUID workspaceId, String nombre, CategoryType type, String icono, String color) {
        this.workspaceId = workspaceId;
        this.nombre = require(nombre, "nombre");
        this.type = type == null ? CategoryType.EXPENSE : type;
        this.icono = icono == null ? "" : icono.trim();
        this.color = normalizeColor(color);
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
    public UUID getWorkspaceId() { return workspaceId; }
    public String getNombre() { return nombre; }
    public CategoryType getType() { return type; }
    public String getIcono() { return icono; }
    public String getColor() { return color; }
    public boolean isArchived() { return archived; }

    public void update(String nombre, String icono, String color) {
        this.nombre = require(nombre, "nombre");
        this.icono = icono == null ? "" : icono.trim();
        this.color = normalizeColor(color);
    }

    public void archive() {
        archived = true;
    }

    public void restore() {
        archived = false;
    }

    private static String normalizeColor(String value) {
        String color = value == null || value.trim().isEmpty() ? "#1a73e8" : value.trim();
        if (!color.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("El color debe usar formato hexadecimal #RRGGBB.");
        }
        return color.toLowerCase(java.util.Locale.ROOT);
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
