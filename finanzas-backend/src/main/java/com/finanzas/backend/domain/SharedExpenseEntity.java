package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shared_expenses")
public class SharedExpenseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "paid_by_user_id", nullable = false)
    private UUID paidByUserId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String description;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false, length = 30)
    private SplitMethod splitMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SharedExpenseStatus status = SharedExpenseStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SharedExpenseEntity() {
    }

    public SharedExpenseEntity(UUID workspaceId, UUID paidByUserId, UUID categoryId, String description,
                               BigDecimal amount, LocalDate expenseDate, SplitMethod splitMethod) {
        this.workspaceId = workspaceId;
        this.paidByUserId = paidByUserId;
        this.categoryId = categoryId;
        this.description = require(description, "descripcion");
        this.amount = amount;
        this.expenseDate = expenseDate == null ? LocalDate.now() : expenseDate;
        this.splitMethod = splitMethod == null ? SplitMethod.EQUAL : splitMethod;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getPaidByUserId() { return paidByUserId; }
    public UUID getCategoryId() { return categoryId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public SplitMethod getSplitMethod() { return splitMethod; }
    public SharedExpenseStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markSettled() {
        status = SharedExpenseStatus.SETTLED;
    }

    public void cancel() {
        status = SharedExpenseStatus.CANCELLED;
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
