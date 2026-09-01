package com.finanzas.api;

public final class BackendInvitation {
    private final String id;
    private final String workspaceId;
    private final String workspaceName;
    private final String invitedEmail;
    private final String invitedByUserId;
    private final String role;
    private final String status;
    private final String expiresAt;
    private final String createdAt;

    BackendInvitation(String id, String workspaceId, String workspaceName, String invitedEmail,
                      String invitedByUserId, String role, String status, String expiresAt,
                      String createdAt) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.workspaceName = workspaceName == null ? "" : workspaceName;
        this.invitedEmail = invitedEmail == null ? "" : invitedEmail;
        this.invitedByUserId = invitedByUserId == null ? "" : invitedByUserId;
        this.role = role == null ? "MEMBER" : role;
        this.status = status == null ? "PENDING" : status;
        this.expiresAt = expiresAt == null ? "" : expiresAt;
        this.createdAt = createdAt == null ? "" : createdAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getWorkspaceName() { return workspaceName; }
    public String getInvitedEmail() { return invitedEmail; }
    public String getInvitedByUserId() { return invitedByUserId; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getExpiresAt() { return expiresAt; }
    public String getCreatedAt() { return createdAt; }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }
}
