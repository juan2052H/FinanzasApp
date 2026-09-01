package com.finanzas.model;

import java.io.Serializable;

public class FinancialCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        INCOME,
        EXPENSE,
        HOUSEHOLD
    }

    private Kind kind;
    private String name;
    private String icon;
    private String color;
    private boolean archived;
    private String backendId = "";
    private String backendWorkspaceId = "";

    public FinancialCategory(Kind kind, String name, String icon, String color) {
        this.kind = kind == null ? Kind.EXPENSE : kind;
        this.name = requireName(name);
        this.icon = icon == null ? "" : icon.trim();
        this.color = normalizeColor(color);
        this.archived = false;
    }

    public Kind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getBackendId() {
        return backendId == null ? "" : backendId;
    }

    public void setBackendId(String backendId) {
        this.backendId = backendId == null ? "" : backendId.trim();
    }

    public String getBackendWorkspaceId() {
        return backendWorkspaceId == null ? "" : backendWorkspaceId;
    }

    public void setBackendWorkspaceId(String backendWorkspaceId) {
        this.backendWorkspaceId = backendWorkspaceId == null ? "" : backendWorkspaceId.trim();
    }

    public void update(String name, String icon, String color) {
        this.name = requireName(name);
        this.icon = icon == null ? "" : icon.trim();
        this.color = normalizeColor(color);
    }

    public void archive() {
        archived = true;
    }

    public void restore() {
        archived = false;
    }

    private String requireName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio.");
        }
        return value.trim();
    }

    private String normalizeColor(String value) {
        String normalized = value == null || value.trim().isEmpty() ? "#1a73e8" : value.trim();
        if (!normalized.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("El color debe usar formato hexadecimal #RRGGBB.");
        }
        return normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
