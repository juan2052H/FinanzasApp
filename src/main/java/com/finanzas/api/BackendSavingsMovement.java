package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendSavingsMovement {
    private final String id;
    private final String workspaceId;
    private final String sourceTransactionId;
    private final String goalId;
    private final String type;
    private final String direction;
    private final BigDecimal amount;
    private final LocalDate effectiveDate;
    private final String note;

    BackendSavingsMovement(String id, String workspaceId, String sourceTransactionId, String goalId,
                           String type, String direction, BigDecimal amount, LocalDate effectiveDate, String note) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.sourceTransactionId = sourceTransactionId == null ? "" : sourceTransactionId;
        this.goalId = goalId == null ? "" : goalId;
        this.type = type == null ? "" : type;
        this.direction = direction == null ? "" : direction;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.effectiveDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        this.note = note == null ? "" : note;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getSourceTransactionId() { return sourceTransactionId; }
    public String getGoalId() { return goalId; }
    public String getType() { return type; }
    public String getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getNote() { return note; }
}
