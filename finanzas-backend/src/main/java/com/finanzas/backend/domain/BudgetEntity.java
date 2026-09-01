package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "budgets")
public class BudgetEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "monto_presupuestado", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetEntity() {
    }

    public BudgetEntity(UUID workspaceId, UUID categoryId, LocalDate periodMonth, BigDecimal amount) {
        this.workspaceId = workspaceId;
        this.categoryId = categoryId;
        this.periodMonth = periodMonth == null ? LocalDate.now().withDayOfMonth(1) : periodMonth.withDayOfMonth(1);
        setAmount(amount);
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
    public UUID getCategoryId() { return categoryId; }
    public LocalDate getPeriodMonth() { return periodMonth; }
    public BigDecimal getAmount() { return amount; }

    public void update(UUID categoryId, LocalDate periodMonth, BigDecimal amount) {
        this.categoryId = categoryId;
        this.periodMonth = periodMonth == null ? LocalDate.now().withDayOfMonth(1) : periodMonth.withDayOfMonth(1);
        setAmount(amount);
    }

    public void setAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El presupuesto no puede ser negativo.");
        }
        this.amount = normalized;
    }
}
