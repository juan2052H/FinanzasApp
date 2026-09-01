package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.InvitationStatus;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceInvitationEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceMemberId;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void ownerCanChangeMemberRole() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        WorkspaceMemberEntity ownerMember = new WorkspaceMemberEntity(workspaceId, ownerId, WorkspaceRole.OWNER);
        WorkspaceMemberEntity targetMember = new WorkspaceMemberEntity(workspaceId, memberId, WorkspaceRole.MEMBER);
        UserEntity memberUser = user(memberId, "Ana", "Lopez", "ana@example.com");

        when(access.requireMember(ownerId, workspaceId)).thenReturn(ownerMember);
        when(members.findById(new WorkspaceMemberId(workspaceId, memberId))).thenReturn(Optional.of(targetMember));
        when(users.findById(memberId)).thenReturn(Optional.of(memberUser));

        HouseholdDtos.MemberResponse response = service.changeRole(
                ownerId,
                workspaceId,
                memberId,
                new HouseholdDtos.MemberRoleRequest(WorkspaceRole.VIEWER));

        assertEquals(WorkspaceRole.VIEWER, response.role());
        assertEquals(WorkspaceRole.VIEWER, targetMember.getRole());
    }

    @Test
    void ownerCanTransferOwnershipToAnotherMember() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        WorkspaceEntity workspace = workspace(workspaceId, ownerId);
        WorkspaceMemberEntity ownerMember = new WorkspaceMemberEntity(workspaceId, ownerId, WorkspaceRole.OWNER);
        WorkspaceMemberEntity targetMember = new WorkspaceMemberEntity(workspaceId, memberId, WorkspaceRole.MEMBER);

        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(members.findById(new WorkspaceMemberId(workspaceId, ownerId))).thenReturn(Optional.of(ownerMember));
        when(members.findById(new WorkspaceMemberId(workspaceId, memberId))).thenReturn(Optional.of(targetMember));

        service.transferOwner(ownerId, workspaceId, new HouseholdDtos.TransferOwnerRequest(memberId));

        assertEquals(memberId, workspace.getOwnerId());
        assertEquals(WorkspaceRole.ADMIN, ownerMember.getRole());
        assertEquals(WorkspaceRole.OWNER, targetMember.getRole());
    }

    @Test
    void ownerCannotLeaveSharedWorkspaceBeforeTransfer() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        WorkspaceEntity workspace = workspace(workspaceId, ownerId);
        WorkspaceMemberEntity ownerMember = new WorkspaceMemberEntity(workspaceId, ownerId, WorkspaceRole.OWNER);
        WorkspaceMemberEntity targetMember = new WorkspaceMemberEntity(workspaceId, memberId, WorkspaceRole.MEMBER);

        when(access.requireMember(ownerId, workspaceId)).thenReturn(ownerMember);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(members.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(ownerMember, targetMember));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.leaveWorkspace(ownerId, workspaceId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(workspaces, never()).delete(workspace);
    }

    @Test
    void nonOwnerCanLeaveWorkspace() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        WorkspaceEntity workspace = workspace(workspaceId, ownerId);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity(workspaceId, memberId, WorkspaceRole.MEMBER);

        when(access.requireMember(memberId, workspaceId)).thenReturn(member);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace));

        service.leaveWorkspace(memberId, workspaceId);

        verify(members).delete(member);
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
