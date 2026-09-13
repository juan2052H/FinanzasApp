package com.finanzas.backend.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SyncDtos {
    private SyncDtos() {
    }

    public record SyncChangesResponse(
            UUID workspaceId,
            Instant revision,
            Instant serverTime,
            List<String> changedResources,
            int pendingReceivedInvitations,
            int pendingWorkspaceInvitations,
            int unreadNotifications) {
    }
}
