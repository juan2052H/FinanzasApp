package com.finanzas.backend.repo;

import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMemberEntity, WorkspaceMemberId> {
    List<WorkspaceMemberEntity> findByIdUserId(UUID userId);
    List<WorkspaceMemberEntity> findByIdWorkspaceId(UUID workspaceId);
    Optional<WorkspaceMemberEntity> findByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);
    boolean existsByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);
}
