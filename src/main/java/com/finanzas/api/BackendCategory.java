package com.finanzas.api;

public final class BackendCategory {
    private final String id;
    private final String workspaceId;
    private final String nombre;
    private final String type;
    private final String icono;
    private final String color;
    private final boolean archived;

    BackendCategory(String id, String workspaceId, String nombre, String type, String icono, String color, boolean archived) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.nombre = nombre == null ? "" : nombre;
        this.type = type == null ? "" : type;
        this.icono = icono == null ? "" : icono;
        this.color = color == null ? "" : color;
        this.archived = archived;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getType() {
        return type;
    }

    public String getIcono() {
        return icono;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }
}
