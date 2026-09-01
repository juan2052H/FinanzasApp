package com.finanzas.domain.model;

import com.finanzas.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class SavingsGoal {
    private final UUID id;
    private final UUID workspaceId;
    private String nombre;
    private BigDecimal currentAmount;
    private BigDecimal targetAmount;
    private LocalDate dueDate;
    private final Instant createdAt;
    private Instant updatedAt;

    public SavingsGoal(UUID id, UUID workspaceId, String nombre, BigDecimal currentAmount,
                       BigDecimal targetAmount, LocalDate dueDate, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.nombre = requireText(nombre, "nombre");
        this.targetAmount = validateTarget(targetAmount);
        setCurrentAmount(currentAmount);
        this.dueDate = dueDate == null ? LocalDate.now().plusMonths(3) : dueDate;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = this.createdAt;
    }

    public static SavingsGoal create(UUID workspaceId, String nombre, BigDecimal targetAmount, LocalDate dueDate) {
        return new SavingsGoal(UUID.randomUUID(), workspaceId, nombre, BigDecimal.ZERO, targetAmount, dueDate, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getNombre() { return nombre; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public BigDecimal remaining() {
        BigDecimal remaining = targetAmount.subtract(currentAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? Money.ZERO : Money.normalize(remaining);
    }

    public BigDecimal progressPercent() {
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal progress = currentAmount.multiply(BigDecimal.valueOf(100)).divide(targetAmount, 2, RoundingMode.HALF_UP);
        return progress.compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP) : progress;
    }

    public void contribute(BigDecimal amount) {
        BigDecimal normalized = Money.normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El aporte debe ser mayor a cero.");
        }
        setCurrentAmount(currentAmount.add(normalized));
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        BigDecimal normalized = Money.normalize(currentAmount);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto actual no puede ser negativo.");
        }
        this.currentAmount = normalized.compareTo(targetAmount) > 0 ? targetAmount : normalized;
        touch();
    }

    private static BigDecimal validateTarget(BigDecimal targetAmount) {
        BigDecimal normalized = Money.normalize(targetAmount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El objetivo debe ser mayor a cero.");
        }
        return normalized;
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
