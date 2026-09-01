package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.domain.WorkspaceType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class WorkspaceDtos {
    private WorkspaceDtos() {
    }

    public record WorkspaceRequest(@NotBlank String nombre, WorkspaceType tipo) {
    }

    public record WorkspaceResponse(UUID id, String nombre, WorkspaceType tipo, UUID ownerId, WorkspaceRole role) {
    }
}
