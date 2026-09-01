package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record ReportResponse(
            UUID workspaceId,
            LocalDate from,
            LocalDate to,
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal balance,
            BigDecimal tasaAhorro,
            BigDecimal ahorroTotal,
            BigDecimal ahorroAsignadoAMetas,
            BigDecimal ahorroLibre,
            BigDecimal saldoDisponibleNoAhorrado,
            Map<String, BigDecimal> gastosPorCategoria,
            List<ReportTransaction> transacciones,
            List<BudgetUsage> presupuestos,
            List<GoalProgress> metas,
            List<RecurringUpcoming> proximosMovimientos) {
    }

    public record ReportTransaction(
            UUID id,
            LocalDate date,
            TransactionType type,
            String category,
            String description,
            BigDecimal amount) {
    }

    public record BudgetUsage(
            UUID budgetId,
            String category,
            LocalDate periodMonth,
            BigDecimal budgeted,
            BigDecimal spent,
            BigDecimal available,
            BigDecimal usagePercent) {
    }

    public record GoalProgress(
            UUID goalId,
            String name,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            BigDecimal progressPercent,
            LocalDate dueDate,
            String status) {
    }

    public record RecurringUpcoming(
            UUID recurringTransactionId,
            LocalDate nextRunDate,
            TransactionType type,
            String description,
            BigDecimal amount) {
    }
}
