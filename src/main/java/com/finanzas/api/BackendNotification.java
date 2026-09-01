package com.finanzas.api;

public final class BackendNotification {
    private final String id;
    private final String workspaceId;
    private final String type;
    private final String title;
    private final String body;

    BackendNotification(String id, String workspaceId, String type, String title, String body) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.type = type == null ? "" : type;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
