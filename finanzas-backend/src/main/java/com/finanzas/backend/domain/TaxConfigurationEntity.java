package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_configurations")
public class TaxConfigurationEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "tax_year", nullable = false)
    private int taxYear;

    @Column(name = "uvt_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal uvtValue;

    @Column(name = "gross_income_uvt", nullable = false)
    private int grossIncomeUvt = 1400;

    @Column(name = "gross_purchases_uvt", nullable = false)
    private int grossPurchasesUvt = 1400;

    @Column(name = "bank_deposits_uvt", nullable = false)
    private int bankDepositsUvt = 1400;

    @Column(name = "gross_wealth_uvt", nullable = false)
    private int grossWealthUvt = 4500;

    @Column(name = "estimated_gross_wealth", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedGrossWealth = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaxConfigurationEntity() {
    }

    public TaxConfigurationEntity(UUID workspaceId,
                                  int taxYear,
                                  BigDecimal uvtValue,
                                  int grossIncomeUvt,
                                  int grossPurchasesUvt,
                                  int bankDepositsUvt,
                                  int grossWealthUvt,
                                  BigDecimal estimatedGrossWealth) {
        this.workspaceId = workspaceId;
        this.taxYear = taxYear;
        this.uvtValue = normalizePositive(uvtValue);
        this.grossIncomeUvt = Math.max(1, grossIncomeUvt);
        this.grossPurchasesUvt = Math.max(1, grossPurchasesUvt);
        this.bankDepositsUvt = Math.max(1, bankDepositsUvt);
        this.grossWealthUvt = Math.max(1, grossWealthUvt);
        this.estimatedGrossWealth = normalizeNonNegative(estimatedGrossWealth);
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
    public int getTaxYear() { return taxYear; }
    public BigDecimal getUvtValue() { return uvtValue; }
    public int getGrossIncomeUvt() { return grossIncomeUvt; }
    public int getGrossPurchasesUvt() { return grossPurchasesUvt; }
    public int getBankDepositsUvt() { return bankDepositsUvt; }
    public int getGrossWealthUvt() { return grossWealthUvt; }
    public BigDecimal getEstimatedGrossWealth() { return estimatedGrossWealth; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(BigDecimal uvtValue,
                       int grossIncomeUvt,
                       int grossPurchasesUvt,
                       int bankDepositsUvt,
                       int grossWealthUvt,
                       BigDecimal estimatedGrossWealth) {
        this.uvtValue = normalizePositive(uvtValue);
        this.grossIncomeUvt = Math.max(1, grossIncomeUvt);
        this.grossPurchasesUvt = Math.max(1, grossPurchasesUvt);
        this.bankDepositsUvt = Math.max(1, bankDepositsUvt);
        this.grossWealthUvt = Math.max(1, grossWealthUvt);
        this.estimatedGrossWealth = normalizeNonNegative(estimatedGrossWealth);
    }

    private static BigDecimal normalizePositive(BigDecimal amount) {
        BigDecimal normalized = normalizeNonNegative(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor de la UVT debe ser mayor a cero.");
        }
        return normalized;
    }

    private static BigDecimal normalizeNonNegative(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
