package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.InvitationStatus;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceInvitationEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceMemberId;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceInvitationRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkspaceCollaborationService {
    private static final Duration INVITATION_TTL = Duration.ofDays(14);

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final WorkspaceInvitationRepository invitations;
    private final UserRepository users;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public WorkspaceCollaborationService(WorkspaceRepository workspaces,
                                         WorkspaceMemberRepository members,
                                         WorkspaceInvitationRepository invitations,
                                         UserRepository users,
                                         WorkspaceAccessService access,
                                         AuditLogService auditLogs) {
        this.workspaces = workspaces;
        this.members = members;
        this.invitations = invitations;
        this.users = users;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<HouseholdDtos.MemberResponse> listMembers(UUID actorUserId, UUID workspaceId) {
        access.requireMember(actorUserId, workspaceId);
        List<WorkspaceMemberEntity> workspaceMembers = members.findByIdWorkspaceId(workspaceId);
        Map<UUID, UserEntity> usersById = usersById(workspaceMembers.stream().map(WorkspaceMemberEntity::getUserId).toList());
        return workspaceMembers.stream()
                .map(member -> toMemberResponse(member, usersById.get(member.getUserId())))
                .toList();
    }

    @Transactional
    public HouseholdDtos.InvitationResponse invite(UUID actorUserId, UUID workspaceId, HouseholdDtos.InvitationRequest request) {
        WorkspaceMemberEntity actor = access.requireMember(actorUserId, workspaceId);
        if (actor.getRole() != WorkspaceRole.OWNER && actor.getRole() != WorkspaceRole.ADMIN) {
            throw forbidden("No tienes permisos para invitar miembros.");
        }
        WorkspaceRole role = normalizeInviteRole(request.role(), actor.getRole());
        String email = UserEntity.normalizeEmail(request.email());
        users.findByEmail(email).ifPresent(user -> {
            if (members.existsByIdWorkspaceIdAndIdUserId(workspaceId, user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya pertenece al workspace.");
            }
        });
        invitations.findByWorkspaceIdAndInvitedEmailAndStatus(workspaceId, email, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una invitacion pendiente para ese correo.");
                });
        WorkspaceInvitationEntity invitation = invitations.save(new WorkspaceInvitationEntity(
                workspaceId,
                email,
                actorUserId,
                role,
                Instant.now().plus(INVITATION_TTL)));
        auditLogs.record(workspaceId, actorUserId, "WORKSPACE_INVITATION_CREATED", "WorkspaceInvitation", invitation.getId(), Map.of(
                "email", email,
                "role", role.name()));
        return toInvitationResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<HouseholdDtos.InvitationResponse> listWorkspaceInvitations(UUID actorUserId, UUID workspaceId) {
        access.requireRole(actorUserId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        return invitations.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public List<HouseholdDtos.InvitationResponse> listMyPendingInvitations(UUID userId) {
        UserEntity user = requireUser(userId);
        Instant now = Instant.now();
        return invitations.findByInvitedEmailAndStatusOrderByCreatedAtDesc(user.getEmail(), InvitationStatus.PENDING).stream()
                .filter(invitation -> expireIfNeeded(invitation, now))
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public HouseholdDtos.InvitationResponse acceptInvitation(UUID userId, UUID invitationId) {
        UserEntity user = requireUser(userId);
        WorkspaceInvitationEntity invitation = requireInvitationForUser(invitationId, user);
        ensureInvitationValid(invitation);
        if (!members.existsByIdWorkspaceIdAndIdUserId(invitation.getWorkspaceId(), userId)) {
            members.save(new WorkspaceMemberEntity(invitation.getWorkspaceId(), userId, invitation.getRole()));
        }
        invitation.accept();
        auditLogs.record(invitation.getWorkspaceId(), userId, "WORKSPACE_INVITATION_ACCEPTED", "WorkspaceInvitation", invitation.getId(), Map.of(
                "role", invitation.getRole().name()));
        return toInvitationResponse(invitation);
    }

    @Transactional
    public HouseholdDtos.InvitationResponse rejectInvitation(UUID userId, UUID invitationId) {
        UserEntity user = requireUser(userId);
        WorkspaceInvitationEntity invitation = requireInvitationForUser(invitationId, user);
        ensureInvitationValid(invitation);
        invitation.reject();
        auditLogs.record(invitation.getWorkspaceId(), userId, "WORKSPACE_INVITATION_REJECTED", "WorkspaceInvitation", invitation.getId(), Map.of());
        return toInvitationResponse(invitation);
    }

    @Transactional
    public HouseholdDtos.InvitationResponse cancelInvitation(UUID actorUserId, UUID workspaceId, UUID invitationId) {
        access.requireRole(actorUserId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        WorkspaceInvitationEntity invitation = invitations.findById(invitationId)
                .filter(candidate -> candidate.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitacion no encontrada."));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes cancelar invitaciones pendientes.");
        }
        invitation.cancel();
        auditLogs.record(workspaceId, actorUserId, "WORKSPACE_INVITATION_CANCELLED", "WorkspaceInvitation", invitation.getId(), Map.of(
                "email", invitation.getInvitedEmail()));
        return toInvitationResponse(invitation);
    }

    @Transactional
    public HouseholdDtos.MemberResponse changeRole(UUID actorUserId, UUID workspaceId, UUID memberUserId, HouseholdDtos.MemberRoleRequest request) {
        WorkspaceMemberEntity actor = access.requireMember(actorUserId, workspaceId);
        WorkspaceMemberEntity target = requireMember(workspaceId, memberUserId);
        WorkspaceRole newRole = normalizeMemberRole(request.role());
        ensureCanManageMember(actor, target, newRole);
        WorkspaceRole previousRole = target.getRole();
        target.changeRole(newRole);
        auditLogs.record(workspaceId, actorUserId, "WORKSPACE_MEMBER_ROLE_CHANGED", "WorkspaceMember", memberUserId, Map.of(
                "previousRole", previousRole.name(),
                "newRole", newRole.name()));
        return toMemberResponse(target, users.findById(memberUserId).orElse(null));
    }

    @Transactional
    public void removeMember(UUID actorUserId, UUID workspaceId, UUID memberUserId) {
        WorkspaceMemberEntity actor = access.requireMember(actorUserId, workspaceId);
        WorkspaceMemberEntity target = requireMember(workspaceId, memberUserId);
        ensureCanManageMember(actor, target, target.getRole());
        if (actorUserId.equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminarte a ti mismo del workspace.");
        }
        members.delete(target);
        auditLogs.record(workspaceId, actorUserId, "WORKSPACE_MEMBER_REMOVED", "WorkspaceMember", memberUserId, Map.of(
                "removedRole", target.getRole().name()));
    }

    private WorkspaceRole normalizeInviteRole(WorkspaceRole role, WorkspaceRole actorRole) {
        WorkspaceRole selected = role == null ? WorkspaceRole.MEMBER : role;
        if (selected == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes invitar miembros como OWNER.");
        }
        if (selected == WorkspaceRole.ADMIN && actorRole != WorkspaceRole.OWNER) {
            throw forbidden("Solo un OWNER puede invitar administradores.");
        }
        return selected;
    }

    private WorkspaceRole normalizeMemberRole(WorkspaceRole role) {
        WorkspaceRole selected = role == null ? WorkspaceRole.MEMBER : role;
        if (selected == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol OWNER no se asigna desde este endpoint.");
        }
        return selected;
    }

    private void ensureCanManageMember(WorkspaceMemberEntity actor, WorkspaceMemberEntity target, WorkspaceRole requestedRole) {
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes modificar al OWNER del workspace.");
        }
        if (actor.getRole() == WorkspaceRole.OWNER) {
            return;
        }
        if (actor.getRole() != WorkspaceRole.ADMIN) {
            throw forbidden("No tienes permisos para administrar miembros.");
        }
        if (target.getRole() == WorkspaceRole.ADMIN || requestedRole == WorkspaceRole.ADMIN) {
            throw forbidden("Un ADMIN no puede administrar otros administradores.");
        }
    }

    private WorkspaceMemberEntity requireMember(UUID workspaceId, UUID userId) {
        return members.findById(new WorkspaceMemberId(workspaceId, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado."));
    }

    private WorkspaceInvitationEntity requireInvitationForUser(UUID invitationId, UserEntity user) {
        return invitations.findById(invitationId)
                .filter(invitation -> invitation.getInvitedEmail().equals(user.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta invitacion no pertenece a tu correo."));
    }

    private void ensureInvitationValid(WorkspaceInvitationEntity invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La invitacion ya fue procesada.");
        }
        if (!invitation.isPendingAndValid(Instant.now())) {
            invitation.expire();
            throw new ResponseStatusException(HttpStatus.GONE, "La invitacion expiro.");
        }
    }

    private boolean expireIfNeeded(WorkspaceInvitationEntity invitation, Instant now) {
        if (invitation.isPendingAndValid(now)) {
            return true;
        }
        invitation.expire();
        return false;
    }

    private UserEntity requireUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
    }

    private Map<UUID, UserEntity> usersById(Collection<UUID> userIds) {
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private HouseholdDtos.MemberResponse toMemberResponse(WorkspaceMemberEntity member, UserEntity user) {
        return new HouseholdDtos.MemberResponse(
                member.getUserId(),
                user == null ? "" : user.getNombre(),
                user == null ? "" : user.getApellido(),
                user == null ? "" : user.getEmail(),
                member.getRole(),
                member.getCreatedAt());
    }

    private HouseholdDtos.InvitationResponse toInvitationResponse(WorkspaceInvitationEntity invitation) {
        WorkspaceEntity workspace = workspaces.findById(invitation.getWorkspaceId()).orElse(null);
        return new HouseholdDtos.InvitationResponse(
                invitation.getId(),
                invitation.getWorkspaceId(),
                workspace == null ? "" : workspace.getNombre(),
                invitation.getInvitedEmail(),
                invitation.getInvitedByUserId(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
