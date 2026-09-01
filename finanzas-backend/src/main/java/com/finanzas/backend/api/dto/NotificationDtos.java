package com.finanzas.backend.api.dto;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID id,
            UUID workspaceId,
            String type,
            String title,
            String body,
            Instant readAt,
            Instant createdAt) {
    }
}
