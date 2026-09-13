package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SyncDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SyncServiceTest {
    private WorkspaceAccessService access;
    private UserRepository users;
    private JdbcTemplate jdbc;
    private SyncService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        access = mock(WorkspaceAccessService.class);
        users = mock(UserRepository.class);
        jdbc = mock(JdbcTemplate.class);
        service = new SyncService(access, users, jdbc);
    }

    @Test
    void changesReportsAllResourcesWhenSinceIsNull() {
        WorkspaceMemberEntity member = new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.OWNER);
        when(access.requireMember(userId, workspaceId)).thenReturn(member);

        UserEntity user = new UserEntity("Test", "User", "test@example.com", "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        Instant now = Instant.now();
        when(jdbc.queryForObject(anyString(), eq(Timestamp.class), any(Object[].class)))
                .thenReturn(Timestamp.from(now));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(0);

        SyncDtos.SyncChangesResponse response = service.changes(userId, workspaceId, null);
        assertNotNull(response);
        assertEquals(workspaceId, response.workspaceId());
        assertNotNull(response.revision());
        assertNotNull(response.changedResources());
        assertEquals(14, response.changedResources().size());
    }

    @Test
    void changesFiltersResourcesAfterSinceTimestamp() {
        WorkspaceMemberEntity member = new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.MEMBER);
        when(access.requireMember(userId, workspaceId)).thenReturn(member);

        UserEntity user = new UserEntity("Test", "User", "test@example.com", "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        Instant past = Instant.parse("2026-01-01T00:00:00Z");
        Instant future = Instant.parse("2026-06-01T00:00:00Z");
        Instant since = Instant.parse("2026-03-01T00:00:00Z");

        when(jdbc.queryForObject(anyString(), eq(Timestamp.class), any(Object[].class)))
                .thenReturn(Timestamp.from(past));
        // Simulate transactions updated after since
        when(jdbc.queryForObject(eq("select max(updated_at) from transactions where workspace_id = ?"), eq(Timestamp.class), any(Object[].class)))
                .thenReturn(Timestamp.from(future));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(0);

        SyncDtos.SyncChangesResponse response = service.changes(userId, workspaceId, since);
        assertNotNull(response);
        assertEquals(1, response.changedResources().size());
        assertEquals("transactions", response.changedResources().get(0));
    }
}
