package com.finanzas.backend.repo;

import com.finanzas.backend.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findTop100ByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
