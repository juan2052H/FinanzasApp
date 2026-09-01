package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AccountDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.RefreshTokenEntity;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.UserSettingsEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.domain.WorkspaceType;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.UserSettingsRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserSettingsRepository settings = mock(UserSettingsRepository.class);
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AvatarStorageService avatars = mock(AvatarStorageService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final UserAccountService service = new UserAccountService(
            users,
            settings,
            workspaces,
            members,
            refreshTokens,
            passwordEncoder,
            avatars,
            auditLogs);

    @Test
    void exportsProfileSettingsSessionsAndWorkspaces() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UserEntity user = user(userId, "ana@example.com");
        UserSettingsEntity userSettings = new UserSettingsEntity(userId, "es-CO");
        RefreshTokenEntity token = new RefreshTokenEntity(userId, "hash", Instant.parse("2026-09-15T00:00:00Z"));
        WorkspaceEntity workspace = workspace(workspaceId, userId);
        WorkspaceMemberEntity membership = new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.OWNER);

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(settings.findById(userId)).thenReturn(Optional.of(userSettings));
        when(refreshTokens.listActive(userId)).thenReturn(List.of(token));
        when(members.findByIdUserId(userId)).thenReturn(List.of(membership));
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(members.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(membership));

        Map<String, Object> exported = service.export(userId);

        assertTrue(exported.containsKey("exportedAt"));
        assertEquals("ana@example.com", profile(exported).get("email"));
        assertEquals("LIGHT", settings(exported).get("theme"));
        assertEquals(1, list(exported, "sessions").size());
        assertEquals("Mi espacio", list(exported, "workspaces").get(0).get("nombre"));
    }

    @Test
    void deleteAnonymizesAccountAndRevokesSessions() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UserEntity user = user(userId, "ana@example.com");
        WorkspaceEntity workspace = workspace(workspaceId, userId);
        WorkspaceMemberEntity membership = new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.OWNER);

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual", "hash")).thenReturn(true);
        when(workspaces.findByOwnerId(userId)).thenReturn(List.of(workspace));
        when(members.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(membership));
        when(members.findByIdUserId(userId)).thenReturn(List.of(membership));
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));

        service.delete(userId, new AccountDtos.DeleteAccountRequest("ANA@EXAMPLE.COM", "actual"));

        assertTrue(user.isDeleted());
        assertEquals("Usuario", user.getNombre());
        assertTrue(user.getEmail().startsWith("deleted+" + userId));
        assertFalse(user.isEmailVerified());
        verify(refreshTokens).revokeAll(userId);
        verify(settings).deleteById(userId);
        verify(avatars).delete(userId);
        verify(workspaces).delete(workspace);
        verify(auditLogs).record(eq(null), eq(userId), eq("USER_ACCOUNT_DELETED"), eq("User"), eq(userId), any());
    }

    @Test
    void deleteBlocksOwnedWorkspaceWithOtherMembers() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UserEntity user = user(userId, "ana@example.com");
        WorkspaceEntity workspace = workspace(workspaceId, userId);

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual", "hash")).thenReturn(true);
        when(workspaces.findByOwnerId(userId)).thenReturn(List.of(workspace));
        when(members.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(
                new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.OWNER),
                new WorkspaceMemberEntity(workspaceId, otherUserId, WorkspaceRole.MEMBER)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(userId, new AccountDtos.DeleteAccountRequest("ana@example.com", "actual")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(refreshTokens, never()).revokeAll(userId);
        verify(workspaces, never()).delete(workspace);
    }

    private UserEntity user(UUID userId, String email) {
        UserEntity user = new UserEntity("Ana", "Lopez", email, "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        ReflectionTestUtils.setField(user, "id", userId);
        user.markEmailVerified(Instant.parse("2026-09-01T00:00:00Z"));
        return user;
    }

    private WorkspaceEntity workspace(UUID workspaceId, UUID ownerId) {
        WorkspaceEntity workspace = new WorkspaceEntity("Mi espacio", WorkspaceType.PERSONAL, ownerId);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);
        return workspace;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> profile(Map<String, Object> exported) {
        return (Map<String, Object>) exported.get("profile");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> settings(Map<String, Object> exported) {
        return (Map<String, Object>) exported.get("settings");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Map<String, Object> exported, String key) {
        return (List<Map<String, Object>>) exported.get(key);
    }
}
