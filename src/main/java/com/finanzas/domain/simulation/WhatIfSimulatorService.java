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

    // ----------------------------------------------------------------
    // Compound interest investment projection
    // ----------------------------------------------------------------

    /**
     * Projects future value of a lump-sum plus monthly contributions compounded monthly.
     *
     * @param principal      Initial amount already invested.
     * @param monthlyContrib Monthly contribution to add.
     * @param annualRatePct  Annual interest rate in percent (e.g. 8.5 for 8.5%).
     * @param months         Number of months to project.
     * @return ProjectionResult with period balance snapshots.
     */
    public ProjectionResult projectCompoundInvestment(BigDecimal principal, BigDecimal monthlyContrib,
                                                      BigDecimal annualRatePct, int months) {
        principal = Money.normalize(principal);
        monthlyContrib = Money.normalize(monthlyContrib);
        BigDecimal monthlyRate = Money.normalize(annualRatePct)
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        int horizon = Math.max(1, months);
        BigDecimal balance = principal;
        BigDecimal totalContrib = principal;
        for (int m = 1; m <= horizon; m++) {
            balance = balance.add(monthlyContrib);
            balance = balance.multiply(BigDecimal.ONE.add(monthlyRate))
                             .setScale(Money.SCALE, RoundingMode.HALF_UP);
            totalContrib = totalContrib.add(monthlyContrib);
        }
        BigDecimal interest = balance.subtract(totalContrib);
        return new ProjectionResult(balance, totalContrib, interest.max(BigDecimal.ZERO), horizon);
    }

    // ----------------------------------------------------------------
    // Debt payoff calculator (snowball / avalanche)
    // ----------------------------------------------------------------

    /**
     * Calculates months to pay off a single debt using a fixed monthly payment.
     *
     * @param balance        Current outstanding balance.
     * @param annualRatePct  Annual interest rate in percent.
     * @param monthlyPayment Monthly payment amount (must exceed monthly interest).
     * @return DebtPayoffResult with months to payoff and total interest paid.
     */
    public DebtPayoffResult calculateDebtPayoff(BigDecimal balance, BigDecimal annualRatePct, BigDecimal monthlyPayment) {
        balance = Money.normalize(balance);
        BigDecimal monthlyRate = Money.normalize(annualRatePct)
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        monthlyPayment = Money.normalize(monthlyPayment);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return new DebtPayoffResult(0, BigDecimal.ZERO);
        }
        BigDecimal monthlyInterestFirst = balance.multiply(monthlyRate).setScale(Money.SCALE, RoundingMode.HALF_UP);
        if (monthlyPayment.compareTo(monthlyInterestFirst) <= 0) {
            // payment doesn't cover interest -> never paid off
            return new DebtPayoffResult(-1, null);
        }
        int month = 0;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal remaining = balance;
        while (remaining.compareTo(BigDecimal.ZERO) > 0 && month < 1200) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(Money.SCALE, RoundingMode.HALF_UP);
            totalInterest = totalInterest.add(interest);
            remaining = remaining.add(interest).subtract(monthlyPayment).max(BigDecimal.ZERO);
            month++;
        }
        return new DebtPayoffResult(month, totalInterest);
    }

    // ----------------------------------------------------------------
    // Emergency fund calculator
    // ----------------------------------------------------------------

    /**
     * Estimates how many months of emergency fund savings are covered.
     *
     * @param currentEmergencyFund Amount already saved for emergencies.
     * @param monthlyExpenses      Average monthly essential expenses.
     * @param targetMonths         Target number of months to cover.
     * @return EmergencyFundResult with coverage and amount still needed.
     */
    public EmergencyFundResult evaluateEmergencyFund(BigDecimal currentEmergencyFund,
                                                     BigDecimal monthlyExpenses,
                                                     int targetMonths) {
        currentEmergencyFund = Money.normalize(currentEmergencyFund);
        monthlyExpenses = Money.normalize(monthlyExpenses);
        int target = Math.max(1, targetMonths);
        BigDecimal targetAmount = monthlyExpenses.multiply(BigDecimal.valueOf(target));
        double coverageMonths = monthlyExpenses.compareTo(BigDecimal.ZERO) > 0
                ? currentEmergencyFund.divide(monthlyExpenses, 4, RoundingMode.HALF_UP).doubleValue()
                : target;
        BigDecimal stillNeeded = targetAmount.subtract(currentEmergencyFund).max(BigDecimal.ZERO);
        return new EmergencyFundResult(coverageMonths, targetAmount, stillNeeded, target);
    }

    // ----------------------------------------------------------------
    // Result value objects
    // ----------------------------------------------------------------

    public static final class ProjectionResult {
        public final BigDecimal finalBalance;
        public final BigDecimal totalContributed;
        public final BigDecimal totalInterestEarned;
        public final int months;

        public ProjectionResult(BigDecimal finalBalance, BigDecimal totalContributed, BigDecimal totalInterestEarned, int months) {
            this.finalBalance = finalBalance;
            this.totalContributed = totalContributed;
            this.totalInterestEarned = totalInterestEarned;
            this.months = months;
        }
    }

    public static final class DebtPayoffResult {
        /** Number of months to pay off. -1 means payment never covers interest. */
        public final int months;
        /** Total interest paid. Null if payment doesn't cover interest. */
        public final BigDecimal totalInterestPaid;

        public DebtPayoffResult(int months, BigDecimal totalInterestPaid) {
            this.months = months;
            this.totalInterestPaid = totalInterestPaid;
        }
    }

    public static final class EmergencyFundResult {
        public final double monthsCovered;
        public final BigDecimal targetAmount;
        public final BigDecimal amountStillNeeded;
        public final int targetMonths;

        public EmergencyFundResult(double monthsCovered, BigDecimal targetAmount, BigDecimal amountStillNeeded, int targetMonths) {
            this.monthsCovered = monthsCovered;
            this.targetAmount = targetAmount;
            this.amountStillNeeded = amountStillNeeded;
            this.targetMonths = targetMonths;
        }
    }
}
