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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "savings_movements")
public class SavingsMovementEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "source_transaction_id")
    private UUID sourceTransactionId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SavingsMovementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SavingsMovementDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, length = 500)
    private String note = "";

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "reversed_movement_id")
    private UUID reversedMovementId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SavingsMovementEntity() {
    }

    public SavingsMovementEntity(UUID workspaceId,
                                 UUID createdByUserId,
                                 UUID sourceTransactionId,
                                 UUID goalId,
                                 SavingsMovementType type,
                                 SavingsMovementDirection direction,
                                 BigDecimal amount,
                                 LocalDate effectiveDate,
                                 String note,
                                 String idempotencyKey,
                                 UUID reversedMovementId) {
        this.workspaceId = workspaceId;
        this.createdByUserId = createdByUserId;
        this.sourceTransactionId = sourceTransactionId;
        this.goalId = goalId;
        this.type = type;
        this.direction = direction;
        this.amount = normalizePositive(amount);
        this.effectiveDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        this.note = note == null ? "" : note.trim();
        this.idempotencyKey = require(idempotencyKey, "idempotencyKey");
        this.reversedMovementId = reversedMovementId;
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
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getSourceTransactionId() { return sourceTransactionId; }
    public UUID getGoalId() { return goalId; }
    public SavingsMovementType getType() { return type; }
    public SavingsMovementDirection getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getNote() { return note; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getReversedMovementId() { return reversedMovementId; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }

    public BigDecimal signedAmount() {
        return direction == SavingsMovementDirection.CREDIT ? amount : amount.negate();
    }

    private static BigDecimal normalizePositive(BigDecimal value) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del movimiento de ahorro debe ser mayor a cero.");
        }
        return normalized;
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
