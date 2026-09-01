package com.finanzas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BackendSettlement {
    private final String id;
    private final String workspaceId;
    private final String fromUserId;
    private final String fromName;
    private final String toUserId;
    private final String toName;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String note;

    BackendSettlement(String id, String workspaceId, String fromUserId, String fromName,
                      String toUserId, String toName, BigDecimal amount, LocalDate date, String note) {
        this.id = id == null ? "" : id;
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.fromUserId = fromUserId == null ? "" : fromUserId;
        this.fromName = fromName == null ? "" : fromName;
        this.toUserId = toUserId == null ? "" : toUserId;
        this.toName = toName == null ? "" : toName;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.date = date == null ? LocalDate.now() : date;
        this.note = note == null ? "" : note;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getFromUserId() { return fromUserId; }
    public String getFromName() { return fromName; }
    public String getToUserId() { return toUserId; }
    public String getToName() { return toName; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
}
