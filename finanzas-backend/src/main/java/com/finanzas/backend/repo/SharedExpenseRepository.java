package com.finanzas.backend.repo;

import com.finanzas.backend.domain.SharedExpenseEntity;
import com.finanzas.backend.domain.SharedExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedExpenseRepository extends JpaRepository<SharedExpenseEntity, UUID> {
    List<SharedExpenseEntity> findByWorkspaceIdOrderByExpenseDateDescCreatedAtDesc(UUID workspaceId);
    List<SharedExpenseEntity> findByWorkspaceIdAndStatusOrderByExpenseDateDescCreatedAtDesc(UUID workspaceId, SharedExpenseStatus status);
    Optional<SharedExpenseEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
