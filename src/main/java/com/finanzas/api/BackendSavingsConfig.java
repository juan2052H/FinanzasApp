package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendSavingsConfig {
    private final String workspaceId;
    private final boolean enabled;
    private final String allocationMode;
    private final BigDecimal percentage;
    private final LocalDate effectiveFrom;
    private final long version;

    BackendSavingsConfig(String workspaceId, boolean enabled, String allocationMode,
                         BigDecimal percentage, LocalDate effectiveFrom, long version) {
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.enabled = enabled;
        this.allocationMode = allocationMode == null ? "" : allocationMode;
        this.percentage = percentage == null ? BigDecimal.ZERO : percentage;
        this.effectiveFrom = effectiveFrom == null ? LocalDate.now() : effectiveFrom;
        this.version = version;
    }

    public String getWorkspaceId() { return workspaceId; }
    public boolean isEnabled() { return enabled; }
    public String getAllocationMode() { return allocationMode; }
    public BigDecimal getPercentage() { return percentage; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public long getVersion() { return version; }
}
