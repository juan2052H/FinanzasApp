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
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false, length = 500)
    private String descripcion = "";

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TransactionEntity() {
    }

    public TransactionEntity(UUID workspaceId, UUID categoryId, UUID createdByUserId, TransactionType type,
                             String descripcion, BigDecimal monto, LocalDate transactionDate) {
        this.workspaceId = workspaceId;
        this.categoryId = categoryId;
        this.createdByUserId = createdByUserId;
        this.type = type == null ? TransactionType.EXPENSE : type;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.monto = normalizeMoney(monto);
        this.transactionDate = transactionDate == null ? LocalDate.now() : transactionDate;
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
    public UUID getCreatedByUserId() { return createdByUserId; }
    public TransactionType getType() { return type; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getTransactionDate() { return transactionDate; }

    public void update(UUID categoryId, TransactionType type, String descripcion, BigDecimal monto, LocalDate transactionDate) {
        this.categoryId = categoryId;
        this.type = type == null ? TransactionType.EXPENSE : type;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.monto = normalizeMoney(monto);
        this.transactionDate = transactionDate == null ? LocalDate.now() : transactionDate;
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        return amount;
    }
}
