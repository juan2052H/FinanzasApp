package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendBudget {
    private final String id;
    private final String workspaceId;
    private final String categoryId;
    private final LocalDate periodMonth;
    private final BigDecimal amount;

    BackendBudget(String id, String workspaceId, String categoryId, LocalDate periodMonth, BigDecimal amount) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.categoryId = categoryId == null ? "" : categoryId;
        this.periodMonth = periodMonth == null ? LocalDate.now().withDayOfMonth(1) : periodMonth.withDayOfMonth(1);
        this.amount = amount == null ? BigDecimal.ZERO : amount;
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

    public LocalDate getPeriodMonth() {
        return periodMonth;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
