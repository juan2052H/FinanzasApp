package com.finanzas.data;

public final class MemberOption {
    private final String userId;
    private final String displayName;
    private final String email;
    private final String role;
    private final String label;

    public MemberOption(String userId, String displayName, String email, String role) {
        this.userId = safe(userId);
        this.displayName = safe(displayName);
        this.email = safe(email);
        this.role = safe(role);
        this.label = buildLabel(this.displayName, this.email, this.userId);
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getLabel() { return label; }

    @Override
    public String toString() {
        return label;
    }

    private String buildLabel(String name, String email, String userId) {
        String visibleName = name.isEmpty() ? email : name;
        if (visibleName.isEmpty()) {
            visibleName = userId.isEmpty() ? "Miembro" : "Miembro " + userId.substring(0, Math.min(8, userId.length()));
        }
        if (!email.isEmpty() && !email.equalsIgnoreCase(visibleName)) {
            return visibleName + " <" + email + ">";
        }
        return visibleName;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
