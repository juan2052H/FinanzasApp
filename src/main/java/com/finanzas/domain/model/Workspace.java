package com.finanzas.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Workspace {
    private final UUID id;
    private String nombre;
    private WorkspaceType tipo;
    private final UUID ownerId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Workspace(UUID id, String nombre, WorkspaceType tipo, UUID ownerId, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = requireText(nombre, "nombre");
        this.tipo = tipo == null ? WorkspaceType.PERSONAL : tipo;
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = this.createdAt;
    }

    public static Workspace personal(String nombre, UUID ownerId) {
        return new Workspace(UUID.randomUUID(), nombre, WorkspaceType.PERSONAL, ownerId, Instant.now());
    }

    public static Workspace household(String nombre, UUID ownerId) {
        return new Workspace(UUID.randomUUID(), nombre, WorkspaceType.HOUSEHOLD, ownerId, Instant.now());
    }

    public static Workspace business(String nombre, UUID ownerId) {
        return new Workspace(UUID.randomUUID(), nombre, WorkspaceType.BUSINESS, ownerId, Instant.now());
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public WorkspaceType getTipo() { return tipo; }
    public UUID getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void rename(String nombre) {
        this.nombre = requireText(nombre, "nombre");
        touch();
    }

    public void changeType(WorkspaceType tipo) {
        this.tipo = tipo == null ? WorkspaceType.PERSONAL : tipo;
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
