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
@Table(name = "savings_config")
public class SavingsConfigEntity {
    public static final BigDecimal DEFAULT_PERCENTAGE = new BigDecimal("20.00");

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private UUID workspaceId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_mode", nullable = false, length = 30)
    private SavingsAllocationMode allocationMode = SavingsAllocationMode.PERCENTAGE;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage = DEFAULT_PERCENTAGE;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SavingsConfigEntity() {
    }

    public SavingsConfigEntity(UUID workspaceId, UUID createdByUserId) {
        this.workspaceId = workspaceId;
        this.createdByUserId = createdByUserId;
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
    public boolean isEnabled() { return enabled; }
    public SavingsAllocationMode getAllocationMode() { return allocationMode; }
    public BigDecimal getPercentage() { return percentage; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public long getVersion() { return version; }

    public void update(Boolean enabled, BigDecimal percentage, LocalDate effectiveFrom, UUID actorUserId) {
        if (enabled != null) {
            this.enabled = enabled.booleanValue();
        }
        if (percentage != null) {
            this.percentage = normalizePercentage(percentage);
        }
        if (effectiveFrom != null) {
            this.effectiveFrom = effectiveFrom;
        }
        if (createdByUserId == null) {
            createdByUserId = actorUserId;
        }
    }

    public static BigDecimal normalizePercentage(BigDecimal value) {
        BigDecimal normalized = value == null ? DEFAULT_PERCENTAGE : value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("El porcentaje de ahorro debe estar entre 0 y 100.");
        }
        return normalized;
    }
}
