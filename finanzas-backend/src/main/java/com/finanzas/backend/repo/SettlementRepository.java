package com.finanzas.backend.repo;

import com.finanzas.backend.domain.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<SettlementEntity, UUID> {
    List<SettlementEntity> findByWorkspaceIdOrderBySettlementDateDescCreatedAtDesc(UUID workspaceId);
}
