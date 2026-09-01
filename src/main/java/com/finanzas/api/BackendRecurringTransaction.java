package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendRecurringTransaction {
    private final String id;
    private final String workspaceId;
    private final String categoryId;
    private final String type;
    private final String description;
    private final BigDecimal amount;
    private final String frequency;
    private final int customIntervalDays;
    private final LocalDate nextRunDate;
    private final boolean active;

    BackendRecurringTransaction(String id, String workspaceId, String categoryId, String type, String description,
                                BigDecimal amount, String frequency, int customIntervalDays,
                                LocalDate nextRunDate, boolean active) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.categoryId = categoryId == null ? "" : categoryId;
        this.type = type == null ? "" : type;
        this.description = description == null ? "" : description;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.frequency = frequency == null ? "" : frequency;
        this.customIntervalDays = Math.max(1, customIntervalDays);
        this.nextRunDate = nextRunDate == null ? LocalDate.now() : nextRunDate;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFrequency() {
        return frequency;
    }

    public int getCustomIntervalDays() {
        return customIntervalDays;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public boolean isActive() {
        return active;
    }
}
