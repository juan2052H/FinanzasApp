package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_members")
public class WorkspaceMemberEntity {
    @EmbeddedId
    private WorkspaceMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkspaceMemberEntity() {
    }

    public WorkspaceMemberEntity(UUID workspaceId, UUID userId, WorkspaceRole role) {
        this.id = new WorkspaceMemberId(workspaceId, userId);
        this.role = role == null ? WorkspaceRole.MEMBER : role;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public WorkspaceMemberId getId() { return id; }
    public UUID getWorkspaceId() { return id.getWorkspaceId(); }
    public UUID getUserId() { return id.getUserId(); }
    public WorkspaceRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }

    public void changeRole(WorkspaceRole role) {
        this.role = role == null ? WorkspaceRole.MEMBER : role;
    }
}
