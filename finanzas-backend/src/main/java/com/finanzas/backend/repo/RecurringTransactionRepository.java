package com.finanzas.backend.repo;

import com.finanzas.backend.domain.RecurringTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
    List<RecurringTransactionEntity> findByWorkspaceIdOrderByNextRunDateAsc(UUID workspaceId);
    List<RecurringTransactionEntity> findByWorkspaceIdAndActiveTrueOrderByNextRunDateAsc(UUID workspaceId);
    List<RecurringTransactionEntity> findByWorkspaceIdAndActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(UUID workspaceId, LocalDate date);
    Optional<RecurringTransactionEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
