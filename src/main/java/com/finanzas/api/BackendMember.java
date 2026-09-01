package com.finanzas.api;

public final class BackendMember {
    private final String userId;
    private final String nombre;
    private final String apellido;
    private final String email;
    private final String role;

    BackendMember(String userId, String nombre, String apellido, String email, String role) {
        this.userId = userId == null ? "" : userId;
        this.nombre = nombre == null ? "" : nombre;
        this.apellido = apellido == null ? "" : apellido;
        this.email = email == null ? "" : email;
        this.role = role == null ? "" : role;
    }

    public String getUserId() { return userId; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public String getDisplayName() {
        String fullName = (nombre + " " + apellido).trim();
        return fullName.isEmpty() ? email : fullName;
    }
}
