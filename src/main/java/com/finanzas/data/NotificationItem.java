package com.finanzas.data;

public final class NotificationItem {
    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        CRITICAL
    }

    private final String title;
    private final String message;
    private final Severity severity;
    private final String section;
    private final String backendId;

    public NotificationItem(String title, String message, Severity severity, String section) {
        this(title, message, severity, section, "");
    }

    public NotificationItem(String title, String message, Severity severity, String section, String backendId) {
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.section = section;
        this.backendId = backendId == null ? "" : backendId;
    }

    /** Id en el servidor; vacio para avisos calculados en el modo local, que no se pueden marcar. */
    public String getBackendId() {
        return backendId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSection() {
        return section;
    }
}
