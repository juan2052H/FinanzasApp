package com.finanzas.domain;

import com.finanzas.domain.analytics.FinancialHealthScore;
import com.finanzas.domain.analytics.FinancialHealthService;
import com.finanzas.domain.model.Budget;
import com.finanzas.domain.model.FinancialTransaction;
import com.finanzas.domain.model.TransactionType;
import com.finanzas.domain.simulation.SimulationResult;
import com.finanzas.domain.simulation.WhatIfSimulatorService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialHealthAndSimulationTest {
    @Test
    void healthScoreUsesOnlyWorkspaceTransactions() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        FinancialHealthScore score = new FinancialHealthService().calculate(
                workspaceA,
                Arrays.asList(
                        FinancialTransaction.create(workspaceA, category, user, TransactionType.INCOME, "Ingreso", new BigDecimal("1000.00"), LocalDate.now()),
                        FinancialTransaction.create(workspaceA, category, user, TransactionType.EXPENSE, "Gasto", new BigDecimal("600.00"), LocalDate.now()),
                        FinancialTransaction.create(workspaceB, category, user, TransactionType.EXPENSE, "No debe contar", new BigDecimal("9999.00"), LocalDate.now())
                ),
                Arrays.asList(Budget.create(workspaceA, category, new BigDecimal("700.00"), YearMonth.now()))
        );

        assertTrue(score.getScore() > 0);
        assertTrue(score.getStrengths().stream().anyMatch(text -> text.contains("Ahorras")));
    }

    @Test
    void whatIfSimulationDoesNotMutateRealDataAndProjectsGoalDate() {
        SimulationResult result = new WhatIfSimulatorService().simulateAdditionalMonthlySavings(
                new BigDecimal("2000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                12);

        assertEquals(new BigDecimal("500.00"), result.getCurrentMonthlySavings());
        assertEquals(new BigDecimal("800.00"), result.getSimulatedMonthlySavings());
        assertEquals(new BigDecimal("300.00"), result.getDifference());
        assertNotNull(result.getProjectedGoalDate());
    }
}
