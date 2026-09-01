package com.finanzas.backend.repo;

import com.finanzas.backend.domain.InvitationStatus;
import com.finanzas.backend.domain.WorkspaceInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitationEntity, UUID> {
    List<WorkspaceInvitationEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    List<WorkspaceInvitationEntity> findByInvitedEmailAndStatusOrderByCreatedAtDesc(String invitedEmail, InvitationStatus status);
    Optional<WorkspaceInvitationEntity> findByWorkspaceIdAndInvitedEmailAndStatus(UUID workspaceId, String invitedEmail, InvitationStatus status);
}
