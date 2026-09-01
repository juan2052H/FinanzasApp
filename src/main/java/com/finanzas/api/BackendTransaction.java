package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendTransaction {
    private final String id;
    private final String workspaceId;
    private final String categoryId;
    private final String createdByUserId;
    private final String type;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate date;

    BackendTransaction(String id, String workspaceId, String categoryId, String createdByUserId,
                       String type, String description, BigDecimal amount, LocalDate date) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.categoryId = categoryId == null ? "" : categoryId;
        this.createdByUserId = createdByUserId == null ? "" : createdByUserId;
        this.type = type == null ? "" : type;
        this.description = description == null ? "" : description;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.date = date == null ? LocalDate.now() : date;
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

    public String getCreatedByUserId() {
        return createdByUserId;
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

    public LocalDate getDate() {
        return date;
    }
}
