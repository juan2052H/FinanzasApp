package com.finanzas.backend.service;

import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkspaceAccessServiceTest {
    @Test
    void rejectsUserWithoutMembership() {
        WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
        WorkspaceAccessService service = new WorkspaceAccessService(members);
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        when(members.findByIdWorkspaceIdAndIdUserId(workspaceId, userId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireMember(userId, workspaceId));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void viewerCannotUseWriteRoleGuard() {
        WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
        WorkspaceAccessService service = new WorkspaceAccessService(members);
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        when(members.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
                .thenReturn(Optional.of(new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.VIEWER)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
}
