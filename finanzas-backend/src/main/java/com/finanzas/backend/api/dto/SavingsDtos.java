package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.SavingsAllocationMode;
import com.finanzas.backend.domain.SavingsMovementDirection;
import com.finanzas.backend.domain.SavingsMovementType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SavingsDtos {
    private SavingsDtos() {
    }

    public record SavingsConfigRequest(
            Boolean enabled,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentage,
            LocalDate effectiveFrom) {
    }

    public record SavingsConfigResponse(
            UUID workspaceId,
            boolean enabled,
            SavingsAllocationMode allocationMode,
            BigDecimal percentage,
            LocalDate effectiveFrom,
            long version) {
    }

    public record SavingsSummaryResponse(
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal ahorroTotal,
            BigDecimal ahorroAsignadoAMetas,
            BigDecimal ahorroLibre,
            BigDecimal retirosAcumulados,
            BigDecimal saldoDisponibleNoAhorrado,
            BigDecimal tasaAhorro) {
    }

    public record SavingsMovementRequest(
            @NotNull @Positive BigDecimal amount,
            LocalDate effectiveDate,
            String note,
            String idempotencyKey) {
    }

    public record GoalSavingsMovementRequest(
            @NotNull @Positive BigDecimal amount,
            String note,
            String idempotencyKey) {
    }

    public record SavingsMovementResponse(
            UUID id,
            UUID workspaceId,
            UUID createdByUserId,
            UUID sourceTransactionId,
            UUID goalId,
            SavingsMovementType type,
            SavingsMovementDirection direction,
            BigDecimal amount,
            LocalDate effectiveDate,
            String note,
            String idempotencyKey,
            UUID reversedMovementId,
            Instant createdAt) {
    }

    public record SavingsMovementPageResponse(
            List<SavingsMovementResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record IncomeImpactResponse(
            BigDecimal incomeAmount,
            BigDecimal percentage,
            BigDecimal automaticSavings,
            BigDecimal availableAfterSavings) {
    }
}
