package com.finanzas.domain.simulation;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SimulationResult {
    private final BigDecimal currentMonthlySavings;
    private final BigDecimal simulatedMonthlySavings;
    private final BigDecimal difference;
    private final BigDecimal projectedAmount;
    private final LocalDate projectedGoalDate;

    public SimulationResult(BigDecimal currentMonthlySavings, BigDecimal simulatedMonthlySavings,
                            BigDecimal difference, BigDecimal projectedAmount, LocalDate projectedGoalDate) {
        this.currentMonthlySavings = currentMonthlySavings;
        this.simulatedMonthlySavings = simulatedMonthlySavings;
        this.difference = difference;
        this.projectedAmount = projectedAmount;
        this.projectedGoalDate = projectedGoalDate;
    }

    public BigDecimal getCurrentMonthlySavings() { return currentMonthlySavings; }
    public BigDecimal getSimulatedMonthlySavings() { return simulatedMonthlySavings; }
    public BigDecimal getDifference() { return difference; }
    public BigDecimal getProjectedAmount() { return projectedAmount; }
    public LocalDate getProjectedGoalDate() { return projectedGoalDate; }
}
