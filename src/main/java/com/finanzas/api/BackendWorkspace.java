package com.finanzas.api;

public final class BackendWorkspace {
    private final String id;
    private final String nombre;
    private final String tipo;
    private final String ownerId;
    private final String role;

    public BackendWorkspace(String id, String nombre, String tipo, String ownerId, String role) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.tipo = tipo == null ? "PERSONAL" : tipo;
        this.ownerId = ownerId == null ? "" : ownerId;
        this.role = role == null ? "MEMBER" : role;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getOwnerId() { return ownerId; }
    public String getRole() { return role; }
}
