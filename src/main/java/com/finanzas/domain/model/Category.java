package com.finanzas.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Category {
    private final UUID id;
    private final UUID workspaceId;
    private String nombre;
    private String icono;
    private String color;
    private boolean archived;
    private final Instant createdAt;
    private Instant updatedAt;

    public Category(UUID id, UUID workspaceId, String nombre, String icono, String color, boolean archived, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.nombre = requireText(nombre, "nombre");
        this.icono = icono == null ? "" : icono;
        this.color = color == null || color.trim().isEmpty() ? "#1a73e8" : color;
        this.archived = archived;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = this.createdAt;
    }

    public static Category create(UUID workspaceId, String nombre, String icono, String color) {
        return new Category(UUID.randomUUID(), workspaceId, nombre, icono, color, false, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public String getColor() { return color; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void rename(String nombre) {
        this.nombre = requireText(nombre, "nombre");
        touch();
    }

    public void updateStyle(String icono, String color) {
        this.icono = icono == null ? "" : icono;
        this.color = color == null || color.trim().isEmpty() ? "#1a73e8" : color;
        touch();
    }

    public void archive() {
        archived = true;
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
