package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.InvitationStatus;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceInvitationEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.domain.WorkspaceType;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceInvitationRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceCollaborationServiceTest {
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final WorkspaceInvitationRepository invitations = mock(WorkspaceInvitationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WorkspaceAccessService access = mock(WorkspaceAccessService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final WorkspaceCollaborationService service = new WorkspaceCollaborationService(
            workspaces,
            members,
            invitations,
            users,
            access,
            auditLogs);

    @Test
    void ownerCanInviteAndInvitedUserCanAccept() {
        UUID ownerId = UUID.randomUUID();
        UUID invitedUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        WorkspaceEntity workspace = workspace(workspaceId, ownerId);
        UserEntity invitedUser = user(invitedUserId, "Ana", "Lopez", "ANA@EXAMPLE.COM");
        WorkspaceMemberEntity ownerMember = new WorkspaceMemberEntity(workspaceId, ownerId, WorkspaceRole.OWNER);
        AtomicReference<WorkspaceInvitationEntity> savedInvitation = new AtomicReference<WorkspaceInvitationEntity>();

        when(access.requireMember(ownerId, workspaceId)).thenReturn(ownerMember);
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(invitedUser));
        when(members.existsByIdWorkspaceIdAndIdUserId(workspaceId, invitedUserId)).thenReturn(false);
        when(invitations.findByWorkspaceIdAndInvitedEmailAndStatus(workspaceId, "ana@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitations.save(any(WorkspaceInvitationEntity.class))).thenAnswer(invocation -> {
            WorkspaceInvitationEntity invitation = invocation.getArgument(0);
            ReflectionTestUtils.setField(invitation, "id", invitationId);
            savedInvitation.set(invitation);
            return invitation;
        });
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));

        HouseholdDtos.InvitationResponse created = service.invite(
                ownerId,
                workspaceId,
                new HouseholdDtos.InvitationRequest("ANA@EXAMPLE.COM", WorkspaceRole.MEMBER));

        assertEquals(invitationId, created.id());
        assertEquals("ana@example.com", created.invitedEmail());
        assertEquals(WorkspaceRole.MEMBER, created.role());
        assertEquals(InvitationStatus.PENDING, created.status());

        when(users.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(invitations.findById(invitationId)).thenReturn(Optional.of(savedInvitation.get()));

        HouseholdDtos.InvitationResponse accepted = service.acceptInvitation(invitedUserId, invitationId);

        assertEquals(InvitationStatus.ACCEPTED, accepted.status());
        verify(members).save(argThat(member ->
                workspaceId.equals(member.getWorkspaceId())
                        && invitedUserId.equals(member.getUserId())
                        && member.getRole() == WorkspaceRole.MEMBER));
    }

    @Test
    void adminCannotInviteAnotherAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        WorkspaceMemberEntity adminMember = new WorkspaceMemberEntity(workspaceId, adminId, WorkspaceRole.ADMIN);

        when(access.requireMember(adminId, workspaceId)).thenReturn(adminMember);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.invite(
                adminId,
                workspaceId,
                new HouseholdDtos.InvitationRequest("admin2@example.com", WorkspaceRole.ADMIN)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void invitedEmailCannotBeAcceptedByAnotherUser() {
        UUID wrongUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        UserEntity wrongUser = user(wrongUserId, "Wrong", "User", "wrong@example.com");
        WorkspaceInvitationEntity invitation = invitation(invitationId, workspaceId, "ana@example.com", UUID.randomUUID(), WorkspaceRole.MEMBER);

        when(users.findById(wrongUserId)).thenReturn(Optional.of(wrongUser));
        when(invitations.findById(invitationId)).thenReturn(Optional.of(invitation));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.acceptInvitation(wrongUserId, invitationId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }

    private WorkspaceEntity workspace(UUID workspaceId, UUID ownerId) {
        WorkspaceEntity workspace = new WorkspaceEntity("Casa", WorkspaceType.HOUSEHOLD, ownerId);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);
        return workspace;
    }

    private WorkspaceInvitationEntity invitation(UUID invitationId, UUID workspaceId, String email,
                                                 UUID invitedByUserId, WorkspaceRole role) {
        WorkspaceInvitationEntity invitation = new WorkspaceInvitationEntity(
                workspaceId,
                email,
                invitedByUserId,
                role,
                Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(invitation, "id", invitationId);
        return invitation;
    }

    private UserEntity user(UUID userId, String nombre, String apellido, String email) {
        UserEntity user = new UserEntity(nombre, apellido, email, "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
