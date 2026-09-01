package com.finanzas.backend.service;

import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceAccessService {
    private final WorkspaceMemberRepository members;

    public WorkspaceAccessService(WorkspaceMemberRepository members) {
        this.members = members;
    }

    public WorkspaceMemberEntity requireMember(UUID userId, UUID workspaceId) {
        return members.findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este workspace."));
    }

    public void requireRole(UUID userId, UUID workspaceId, WorkspaceRole... roles) {
        WorkspaceMemberEntity member = requireMember(userId, workspaceId);
        Set<WorkspaceRole> allowed = Set.of(roles);
        if (!allowed.contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos suficientes.");
        }
    }
}
