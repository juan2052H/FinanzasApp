package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.WorkspaceDtos;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import com.finanzas.backend.service.AuditLogService;
import com.finanzas.backend.service.DefaultCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final DefaultCategoryService defaultCategories;
    private final AuditLogService auditLogs;

    public WorkspaceController(WorkspaceRepository workspaces,
                               WorkspaceMemberRepository members,
                               DefaultCategoryService defaultCategories,
                               AuditLogService auditLogs) {
        this.workspaces = workspaces;
        this.members = members;
        this.defaultCategories = defaultCategories;
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<WorkspaceDtos.WorkspaceResponse> list(Authentication authentication) {
        UUID userId = CurrentUser.id(authentication);
        return members.findByIdUserId(userId).stream()
                .map(member -> toResponse(workspaces.findById(member.getWorkspaceId()).orElseThrow(), member.getRole()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public WorkspaceDtos.WorkspaceResponse create(Authentication authentication, @Valid @RequestBody WorkspaceDtos.WorkspaceRequest request) {
        UUID userId = CurrentUser.id(authentication);
        WorkspaceEntity workspace = workspaces.save(new WorkspaceEntity(request.nombre(), request.tipo(), userId));
        members.save(new WorkspaceMemberEntity(workspace.getId(), userId, WorkspaceRole.OWNER));
        defaultCategories.seed(workspace.getId());
        auditLogs.record(workspace.getId(), userId, "WORKSPACE_CREATED", "Workspace", workspace.getId(), Map.of(
                "tipo", workspace.getTipo().name(),
                "nombre", workspace.getNombre()));
        return toResponse(workspace, WorkspaceRole.OWNER);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceDtos.WorkspaceResponse get(Authentication authentication, @PathVariable UUID workspaceId) {
        UUID userId = CurrentUser.id(authentication);
        WorkspaceMemberEntity member = members.findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este workspace."));
        WorkspaceEntity workspace = workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace no encontrado."));
        return toResponse(workspace, member.getRole());
    }

    private WorkspaceDtos.WorkspaceResponse toResponse(WorkspaceEntity workspace, WorkspaceRole role) {
        return new WorkspaceDtos.WorkspaceResponse(workspace.getId(), workspace.getNombre(), workspace.getTipo(), workspace.getOwnerId(), role);
    }
}
