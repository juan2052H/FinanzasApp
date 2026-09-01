package com.finanzas.domain.model;

import com.finanzas.model.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class FinancialTransaction {
    private final UUID id;
    private final UUID workspaceId;
    private final UUID categoryId;
    private final UUID createdByUserId;
    private TransactionType tipo;
    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;
    private final Instant createdAt;
    private Instant updatedAt;

    public FinancialTransaction(UUID id, UUID workspaceId, UUID categoryId, UUID createdByUserId,
                                TransactionType tipo, String descripcion, BigDecimal monto,
                                LocalDate fecha, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.tipo = tipo == null ? TransactionType.EXPENSE : tipo;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        setMonto(monto);
        this.fecha = fecha == null ? LocalDate.now() : fecha;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = this.createdAt;
    }

    public static FinancialTransaction create(UUID workspaceId, UUID categoryId, UUID userId,
                                              TransactionType tipo, String descripcion,
                                              BigDecimal monto, LocalDate fecha) {
        return new FinancialTransaction(UUID.randomUUID(), workspaceId, categoryId, userId, tipo, descripcion, monto, fecha, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public TransactionType getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(TransactionType tipo, String descripcion, BigDecimal monto, LocalDate fecha) {
        this.tipo = tipo == null ? this.tipo : tipo;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        setMonto(monto);
        this.fecha = fecha == null ? this.fecha : fecha;
        touch();
    }

    private void setMonto(BigDecimal monto) {
        BigDecimal normalized = Money.normalize(monto);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        this.monto = normalized;
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
