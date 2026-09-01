package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class BackendSharedExpense {
    private final String id;
    private final String workspaceId;
    private final String paidByUserId;
    private final String paidByName;
    private final String categoryId;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String splitMethod;
    private final String status;
    private final List<BackendExpenseSplit> splits;

    BackendSharedExpense(String id, String workspaceId, String paidByUserId, String paidByName,
                         String categoryId, String description, BigDecimal amount, LocalDate date,
                         String splitMethod, String status, List<BackendExpenseSplit> splits) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.paidByUserId = paidByUserId == null ? "" : paidByUserId;
        this.paidByName = paidByName == null ? "" : paidByName;
        this.categoryId = categoryId == null ? "" : categoryId;
        this.description = description == null ? "" : description;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.date = date == null ? LocalDate.now() : date;
        this.splitMethod = splitMethod == null ? "" : splitMethod;
        this.status = status == null ? "" : status;
        this.splits = splits == null ? new ArrayList<BackendExpenseSplit>() : new ArrayList<BackendExpenseSplit>(splits);
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getPaidByUserId() { return paidByUserId; }
    public String getPaidByName() { return paidByName; }
    public String getCategoryId() { return categoryId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getSplitMethod() { return splitMethod; }
    public String getStatus() { return status; }
    public List<BackendExpenseSplit> getSplits() { return new ArrayList<BackendExpenseSplit>(splits); }
}
