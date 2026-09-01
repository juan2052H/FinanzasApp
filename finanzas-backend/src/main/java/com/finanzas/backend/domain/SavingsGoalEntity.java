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
@Table(name = "savings_goals")
public class SavingsGoalEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "monto_actual", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "monto_objetivo", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, length = 16)
    private String color = "#1a73e8";

    @Column(nullable = false, length = 80)
    private String icono = "";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavingsGoalEntity() {
    }

    public SavingsGoalEntity(UUID workspaceId, String nombre, BigDecimal currentAmount, BigDecimal targetAmount,
                             String color, String icono, LocalDate dueDate) {
        this.workspaceId = workspaceId;
        this.nombre = require(nombre, "nombre");
        this.currentAmount = normalizeNonNegative(currentAmount);
        this.targetAmount = normalizePositive(targetAmount);
        this.color = color == null || color.isBlank() ? "#1a73e8" : color.trim();
        this.icono = icono == null ? "" : icono.trim();
        this.dueDate = dueDate;
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
    public String getNombre() { return nombre; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public String getColor() { return color; }
    public String getIcono() { return icono; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }

    public void update(String nombre, BigDecimal currentAmount, BigDecimal targetAmount,
                       String color, String icono, LocalDate dueDate) {
        this.nombre = require(nombre, "nombre");
        this.currentAmount = normalizeNonNegative(currentAmount);
        this.targetAmount = normalizePositive(targetAmount);
        if (this.currentAmount.compareTo(this.targetAmount) > 0) {
            this.currentAmount = this.targetAmount;
        }
        this.color = color == null || color.isBlank() ? "#1a73e8" : color.trim();
        this.icono = icono == null ? "" : icono.trim();
        this.dueDate = dueDate;
        this.status = this.currentAmount.compareTo(this.targetAmount) >= 0 ? "COMPLETED" : "ACTIVE";
    }

    public void contribute(BigDecimal amount) {
        currentAmount = currentAmount.add(normalizePositive(amount)).min(targetAmount).setScale(2, java.math.RoundingMode.HALF_UP);
        if (currentAmount.compareTo(targetAmount) >= 0) {
            status = "COMPLETED";
        }
    }

    public void allocate(BigDecimal amount) {
        contribute(amount);
    }

    public void release(BigDecimal amount) {
        BigDecimal normalized = normalizePositive(amount);
        if (currentAmount.compareTo(normalized) < 0) {
            throw new IllegalArgumentException("No puedes liberar mas de lo asignado a la meta.");
        }
        currentAmount = currentAmount.subtract(normalized).setScale(2, java.math.RoundingMode.HALF_UP);
        if (currentAmount.compareTo(targetAmount) < 0 && "COMPLETED".equals(status)) {
            status = "ACTIVE";
        }
    }

    public void archive() {
        status = "ARCHIVED";
    }

    private static BigDecimal normalizePositive(BigDecimal amount) {
        BigDecimal normalized = normalizeNonNegative(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        return normalized;
    }

    private static BigDecimal normalizeNonNegative(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo.");
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
