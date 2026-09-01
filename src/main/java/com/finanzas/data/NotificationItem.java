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

    public NotificationItem(String title, String message, Severity severity, String section) {
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.section = section;
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
