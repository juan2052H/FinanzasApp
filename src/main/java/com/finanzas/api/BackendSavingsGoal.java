package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendSavingsGoal {
    private final String id;
    private final String workspaceId;
    private final String name;
    private final BigDecimal currentAmount;
    private final BigDecimal targetAmount;
    private final String color;
    private final String icono;
    private final LocalDate dueDate;
    private final String status;

    BackendSavingsGoal(String id, String workspaceId, String name, BigDecimal currentAmount,
                       BigDecimal targetAmount, String color, String icono, LocalDate dueDate, String status) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.name = name == null ? "" : name;
        this.currentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        this.targetAmount = targetAmount == null ? BigDecimal.ZERO : targetAmount;
        this.color = color == null ? "" : color;
        this.icono = icono == null ? "" : icono;
        this.dueDate = dueDate == null ? LocalDate.now().plusMonths(6) : dueDate;
        this.status = status == null ? "" : status;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public String getColor() {
        return color;
    }

    public String getIcono() {
        return icono;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }
}
