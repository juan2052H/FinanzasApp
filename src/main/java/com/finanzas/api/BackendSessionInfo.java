package com.finanzas.api;

import java.time.Instant;

public final class BackendSessionInfo {
    private final String id;
    private final Instant createdAt;
    private final Instant lastUsedAt;
    private final Instant expiresAt;

    public BackendSessionInfo(String id, Instant createdAt, Instant lastUsedAt, Instant expiresAt) {
        this.id = id == null ? "" : id;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
