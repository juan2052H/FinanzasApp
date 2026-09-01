package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.AuditDtos;
import com.finanzas.backend.service.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogs;

    public AuditLogController(AuditLogService auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<AuditDtos.AuditLogResponse> list(Authentication authentication, @PathVariable UUID workspaceId) {
        return auditLogs.listWorkspaceLogs(CurrentUser.id(authentication), workspaceId);
    }
}
