package com.finanzas.domain;

import com.finanzas.domain.model.WorkspaceMember;
import com.finanzas.domain.model.WorkspaceRole;
import com.finanzas.domain.security.WorkspaceAccessException;
import com.finanzas.domain.security.WorkspaceAccessService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceAccessServiceTest {
    @Test
    void userCannotAccessAnotherWorkspaceWithoutMembership() {
        WorkspaceAccessService service = new WorkspaceAccessService();
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        assertTrue(service.canRead(userA, workspaceA, Arrays.asList(
                new WorkspaceMember(workspaceA, userA, WorkspaceRole.OWNER, null)
        )));

        assertThrows(WorkspaceAccessException.class, () -> service.requireMembership(userB, workspaceA, Arrays.asList(
                new WorkspaceMember(workspaceB, userB, WorkspaceRole.OWNER, null)
        )));
    }

    @Test
    void viewerCannotWriteFinancialData() {
        WorkspaceAccessService service = new WorkspaceAccessService();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThrows(WorkspaceAccessException.class, () -> service.requireFinancialWrite(userId, workspaceId, Arrays.asList(
                new WorkspaceMember(workspaceId, userId, WorkspaceRole.VIEWER, null)
        )));
    }
}
