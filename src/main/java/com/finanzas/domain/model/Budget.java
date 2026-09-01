package com.finanzas.domain.model;

import com.finanzas.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public final class Budget {
    private final UUID id;
    private final UUID workspaceId;
    private final UUID categoryId;
    private BigDecimal monthlyLimit;
    private YearMonth period;
    private final Instant createdAt;
    private Instant updatedAt;

    public Budget(UUID id, UUID workspaceId, UUID categoryId, BigDecimal monthlyLimit, YearMonth period, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        setMonthlyLimit(monthlyLimit);
        this.period = period == null ? YearMonth.now() : period;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = this.createdAt;
    }

    public static Budget create(UUID workspaceId, UUID categoryId, BigDecimal monthlyLimit, YearMonth period) {
        return new Budget(UUID.randomUUID(), workspaceId, categoryId, monthlyLimit, period, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getCategoryId() { return categoryId; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public YearMonth getPeriod() { return period; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public BigDecimal spentPercentage(BigDecimal spent) {
        if (monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return Money.normalize(spent)
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyLimit, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal available(BigDecimal spent) {
        return Money.normalize(monthlyLimit.subtract(Money.normalize(spent)));
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        BigDecimal normalized = Money.normalize(monthlyLimit);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El presupuesto no puede ser negativo.");
        }
        this.monthlyLimit = normalized;
        touch();
    }

    public void moveToPeriod(YearMonth period) {
        this.period = period == null ? YearMonth.now() : period;
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
