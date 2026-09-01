package com.finanzas.backend.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditLogResponse(
            UUID id,
            UUID workspaceId,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> metadata,
            Instant createdAt) {
    }
}
