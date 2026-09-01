package com.finanzas.backend.repo;

import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByWorkspaceIdOrderByTransactionDateDesc(UUID workspaceId);
    List<TransactionEntity> findByWorkspaceIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(UUID workspaceId, TransactionType type, LocalDate from, LocalDate to);
    List<TransactionEntity> findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(UUID workspaceId, LocalDate from, LocalDate to);
    Optional<TransactionEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    boolean existsByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
