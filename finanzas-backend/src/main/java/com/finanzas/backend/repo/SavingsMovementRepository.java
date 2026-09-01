package com.finanzas.backend.repo;

import com.finanzas.backend.domain.SavingsMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsMovementRepository extends JpaRepository<SavingsMovementEntity, UUID> {
    Optional<SavingsMovementEntity> findByWorkspaceIdAndIdempotencyKey(UUID workspaceId, String idempotencyKey);
    List<SavingsMovementEntity> findByWorkspaceId(UUID workspaceId);
    List<SavingsMovementEntity> findByWorkspaceIdAndSourceTransactionId(UUID workspaceId, UUID sourceTransactionId);
    Page<SavingsMovementEntity> findByWorkspaceIdOrderByEffectiveDateDescCreatedAtDesc(UUID workspaceId, Pageable pageable);
}
