package com.finanzas.backend.repo;

import com.finanzas.backend.domain.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
    List<WorkspaceEntity> findByOwnerId(UUID ownerId);
}
