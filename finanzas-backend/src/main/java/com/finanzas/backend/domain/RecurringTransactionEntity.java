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
@Table(name = "recurring_transactions")
public class RecurringTransactionEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TransactionType type;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String description = "";

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecurringFrequency frequency;

    @Column(name = "custom_interval_days", nullable = false)
    private int customIntervalDays = 1;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringTransactionEntity() {
    }

    public RecurringTransactionEntity(UUID workspaceId, UUID categoryId, TransactionType type, String description,
                                      BigDecimal amount, RecurringFrequency frequency, Integer customIntervalDays,
                                      LocalDate nextRunDate) {
        this.workspaceId = workspaceId;
        this.categoryId = categoryId;
        this.type = type == null ? TransactionType.EXPENSE : type;
        this.description = description == null ? "" : description.trim();
        this.amount = normalizeMoney(amount);
        this.frequency = frequency == null ? RecurringFrequency.MONTHLY : frequency;
        this.customIntervalDays = customIntervalDays == null ? 1 : Math.max(1, customIntervalDays);
        this.nextRunDate = nextRunDate == null ? LocalDate.now() : nextRunDate;
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
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public RecurringFrequency getFrequency() { return frequency; }
    public int getCustomIntervalDays() { return customIntervalDays; }
    public LocalDate getNextRunDate() { return nextRunDate; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void deactivate() {
        active = false;
    }

    public void advanceNextRunDate() {
        nextRunDate = switch (frequency) {
            case WEEKLY -> nextRunDate.plusWeeks(1);
            case BIWEEKLY -> nextRunDate.plusWeeks(2);
            case CUSTOM -> nextRunDate.plusDays(customIntervalDays);
            case MONTHLY -> nextRunDate.plusMonths(1);
            case YEARLY -> nextRunDate.plusYears(1);
        };
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        return normalized;
    }
}
