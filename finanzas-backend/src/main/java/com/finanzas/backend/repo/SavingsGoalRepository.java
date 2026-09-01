package com.finanzas.backend.repo;

import com.finanzas.backend.domain.SavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, UUID> {
    List<SavingsGoalEntity> findByWorkspaceIdAndStatusNotOrderByDueDateAsc(UUID workspaceId, String status);
    Optional<SavingsGoalEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
