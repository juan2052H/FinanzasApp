package com.finanzas.domain.security;

import com.finanzas.domain.model.WorkspaceMember;
import com.finanzas.domain.model.WorkspaceRole;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class WorkspaceAccessService {
    public WorkspaceMember requireMembership(UUID userId, UUID workspaceId, Collection<WorkspaceMember> memberships) {
        return findMembership(userId, workspaceId, memberships)
                .orElseThrow(() -> new WorkspaceAccessException("El usuario no pertenece a este workspace."));
    }

    public WorkspaceMember requireFinancialWrite(UUID userId, UUID workspaceId, Collection<WorkspaceMember> memberships) {
        WorkspaceMember member = requireMembership(userId, workspaceId, memberships);
        if (!member.getRole().canWriteFinancialData()) {
            throw new WorkspaceAccessException("El rol " + member.getRole() + " no puede modificar informacion financiera.");
        }
        return member;
    }

    public WorkspaceMember requireMemberManagement(UUID userId, UUID workspaceId, Collection<WorkspaceMember> memberships) {
        WorkspaceMember member = requireMembership(userId, workspaceId, memberships);
        if (!member.getRole().canManageMembers()) {
            throw new WorkspaceAccessException("El rol " + member.getRole() + " no puede administrar miembros.");
        }
        return member;
    }

    public boolean canRead(UUID userId, UUID workspaceId, Collection<WorkspaceMember> memberships) {
        return findMembership(userId, workspaceId, memberships).isPresent();
    }

    private Optional<WorkspaceMember> findMembership(UUID userId, UUID workspaceId, Collection<WorkspaceMember> memberships) {
        if (userId == null || workspaceId == null || memberships == null) {
            return Optional.empty();
        }
        return memberships.stream()
                .filter(member -> workspaceId.equals(member.getWorkspaceId()))
                .filter(member -> userId.equals(member.getUserId()))
                .findFirst();
    }
}
