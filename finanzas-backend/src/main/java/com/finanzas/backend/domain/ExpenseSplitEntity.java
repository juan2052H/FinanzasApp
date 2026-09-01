package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplitEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "shared_expense_id", nullable = false)
    private UUID sharedExpenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 7, scale = 4)
    private BigDecimal percentage;

    @Column(name = "settled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExpenseSplitEntity() {
    }

    public ExpenseSplitEntity(UUID sharedExpenseId, UUID userId, BigDecimal amount, BigDecimal percentage) {
        this.sharedExpenseId = sharedExpenseId;
        this.userId = userId;
        this.amount = amount;
        this.percentage = percentage;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSharedExpenseId() { return sharedExpenseId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public Instant getCreatedAt() { return createdAt; }

    public BigDecimal outstandingAmount() {
        return amount.subtract(settledAmount);
    }

    public void applySettlement(BigDecimal settlementAmount) {
        if (settlementAmount == null || settlementAmount.signum() <= 0) {
            return;
        }
        BigDecimal outstanding = outstandingAmount();
        BigDecimal applied = settlementAmount.min(outstanding);
        settledAmount = settledAmount.add(applied);
    }
}
