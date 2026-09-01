package com.finanzas.domain.simulation;

import com.finanzas.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class WhatIfSimulatorService {
    public SimulationResult simulateAdditionalMonthlySavings(BigDecimal currentMonthlyIncome,
                                                             BigDecimal currentMonthlyExpenses,
                                                             BigDecimal additionalSavings,
                                                             BigDecimal currentSavedAmount,
                                                             BigDecimal goalAmount,
                                                             int months) {
        BigDecimal income = Money.normalize(currentMonthlyIncome);
        BigDecimal expenses = Money.normalize(currentMonthlyExpenses);
        BigDecimal additional = Money.normalize(additionalSavings);
        BigDecimal saved = Money.normalize(currentSavedAmount);
        BigDecimal goal = Money.normalize(goalAmount);
        int horizon = Math.max(1, months);

        BigDecimal currentMonthlySavings = Money.normalize(income.subtract(expenses));
        BigDecimal simulatedMonthlySavings = Money.normalize(currentMonthlySavings.add(additional));
        BigDecimal projectedAmount = Money.normalize(saved.add(simulatedMonthlySavings.multiply(BigDecimal.valueOf(horizon))));
        LocalDate projectedGoalDate = projectedDate(saved, goal, simulatedMonthlySavings);

        return new SimulationResult(
                currentMonthlySavings,
                simulatedMonthlySavings,
                Money.normalize(simulatedMonthlySavings.subtract(currentMonthlySavings)),
                projectedAmount,
                projectedGoalDate);
    }

    public SimulationResult simulateExpenseReduction(BigDecimal currentMonthlyIncome,
                                                     BigDecimal currentMonthlyExpenses,
                                                     BigDecimal reductionPercent,
                                                     BigDecimal currentSavedAmount,
                                                     BigDecimal goalAmount,
                                                     int months) {
        BigDecimal reduction = Money.normalize(currentMonthlyExpenses)
                .multiply(Money.normalize(reductionPercent))
                .divide(BigDecimal.valueOf(100), Money.SCALE, RoundingMode.HALF_UP);
        return simulateAdditionalMonthlySavings(
                currentMonthlyIncome,
                currentMonthlyExpenses,
                reduction,
                currentSavedAmount,
                goalAmount,
                months);
    }

    private LocalDate projectedDate(BigDecimal currentSavedAmount, BigDecimal goalAmount, BigDecimal monthlySavings) {
        if (goalAmount.compareTo(BigDecimal.ZERO) <= 0 || currentSavedAmount.compareTo(goalAmount) >= 0) {
            return LocalDate.now();
        }
        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal remaining = goalAmount.subtract(currentSavedAmount);
        int months = remaining.divide(monthlySavings, 0, RoundingMode.CEILING).intValue();
        return LocalDate.now().plusMonths(months);
    }
}
