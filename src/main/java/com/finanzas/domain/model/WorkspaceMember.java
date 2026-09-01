package com.finanzas.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class WorkspaceMember {
    private final UUID workspaceId;
    private final UUID userId;
    private WorkspaceRole role;
    private final Instant createdAt;

    public WorkspaceMember(UUID workspaceId, UUID userId, WorkspaceRole role, Instant createdAt) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.role = role == null ? WorkspaceRole.MEMBER : role;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static WorkspaceMember owner(UUID workspaceId, UUID userId) {
        return new WorkspaceMember(workspaceId, userId, WorkspaceRole.OWNER, Instant.now());
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getUserId() { return userId; }
    public WorkspaceRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }

    public void changeRole(WorkspaceRole role) {
        if (this.role == WorkspaceRole.OWNER && role != WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("No se puede degradar al propietario sin transferir la propiedad.");
        }
        this.role = role == null ? WorkspaceRole.MEMBER : role;
    }
}
