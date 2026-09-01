package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AuditDtos;
import com.finanzas.backend.domain.AuditLogEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogs;
    private final WorkspaceAccessService access;

    public AuditLogService(AuditLogRepository auditLogs, WorkspaceAccessService access) {
        this.auditLogs = auditLogs;
        this.access = access;
    }

    @Transactional
    public void record(UUID workspaceId, UUID actorUserId, String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        auditLogs.save(new AuditLogEntity(workspaceId, actorUserId, action, entityType, entityId, metadata));
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.AuditLogResponse> listWorkspaceLogs(UUID actorUserId, UUID workspaceId) {
        access.requireRole(actorUserId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        return auditLogs.findTop100ByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditDtos.AuditLogResponse toResponse(AuditLogEntity log) {
        return new AuditDtos.AuditLogResponse(
                log.getId(),
                log.getWorkspaceId(),
                log.getActorUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getCreatedAt());
    }
}
