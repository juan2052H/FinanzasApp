package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.SyncDtos;
import com.finanzas.backend.service.SyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/sync")
public class SyncController {
    private final SyncService sync;

    public SyncController(SyncService sync) {
        this.sync = sync;
    }

    @GetMapping("/changes")
    public SyncDtos.SyncChangesResponse changes(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return sync.changes(CurrentUser.id(authentication), workspaceId, since);
    }
}
