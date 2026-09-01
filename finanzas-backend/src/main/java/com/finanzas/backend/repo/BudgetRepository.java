package com.finanzas.backend.repo;

import com.finanzas.backend.domain.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    List<BudgetEntity> findByWorkspaceIdOrderByPeriodMonthDesc(UUID workspaceId);
    Optional<BudgetEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
