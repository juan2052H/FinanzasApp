package com.finanzas.domain.analytics;

import com.finanzas.domain.model.Budget;
import com.finanzas.domain.model.FinancialTransaction;
import com.finanzas.domain.model.TransactionType;
import com.finanzas.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class FinancialHealthService {
    public FinancialHealthScore calculate(UUID workspaceId, List<FinancialTransaction> transactions, List<Budget> budgets) {
        List<FinancialTransaction> scoped = transactions.stream()
                .filter(transaction -> workspaceId.equals(transaction.getWorkspaceId()))
                .collect(Collectors.toList());

        BigDecimal income = total(scoped, TransactionType.INCOME);
        BigDecimal expenses = total(scoped, TransactionType.EXPENSE);
        BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? income.subtract(expenses).multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int score = 0;
        List<String> strengths = new ArrayList<String>();
        List<String> opportunities = new ArrayList<String>();

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            opportunities.add("Registra ingresos para calcular tu tasa de ahorro.");
        } else if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            score += 35;
            strengths.add("Ahorras " + savingsRate + "% de tus ingresos.");
        } else if (savingsRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
            score += 25;
            strengths.add("Mantienes una tasa de ahorro positiva de " + savingsRate + "%.");
        } else if (savingsRate.compareTo(BigDecimal.ZERO) > 0) {
            score += 15;
            opportunities.add("Tu tasa de ahorro es " + savingsRate + "%; intenta acercarla a 20%.");
        } else {
            opportunities.add("Tus gastos igualan o superan tus ingresos.");
        }

        BudgetResult budgetResult = evaluateBudgets(workspaceId, scoped, budgets);
        score += budgetResult.score;
        strengths.addAll(budgetResult.strengths);
        opportunities.addAll(budgetResult.opportunities);

        if (expenses.compareTo(income) <= 0 && income.compareTo(BigDecimal.ZERO) > 0) {
            score += 20;
            strengths.add("Tu flujo de caja acumulado es positivo.");
        } else if (income.compareTo(BigDecimal.ZERO) > 0) {
            opportunities.add("Reduce gastos para recuperar flujo de caja positivo.");
        }

        return new FinancialHealthScore(score, labelFor(score), strengths, opportunities);
    }

    private BudgetResult evaluateBudgets(UUID workspaceId, List<FinancialTransaction> scopedTransactions, List<Budget> budgets) {
        List<Budget> scopedBudgets = budgets.stream()
                .filter(budget -> workspaceId.equals(budget.getWorkspaceId()))
                .collect(Collectors.toList());
        if (scopedBudgets.isEmpty()) {
            return new BudgetResult(10, new ArrayList<String>(), list("Crea presupuestos para medir cumplimiento."));
        }

        YearMonth current = YearMonth.now();
        Map<UUID, BigDecimal> spentByCategory = scopedTransactions.stream()
                .filter(transaction -> transaction.getTipo() == TransactionType.EXPENSE)
                .filter(transaction -> YearMonth.from(transaction.getFecha()).equals(current))
                .collect(Collectors.groupingBy(
                        FinancialTransaction::getCategoryId,
                        Collectors.reducing(Money.ZERO, FinancialTransaction::getMonto, BigDecimal::add)));

        int ok = 0;
        int exceeded = 0;
        for (Budget budget : scopedBudgets) {
            BigDecimal spent = spentByCategory.getOrDefault(budget.getCategoryId(), Money.ZERO);
            if (budget.spentPercentage(spent).compareTo(BigDecimal.valueOf(100)) <= 0) {
                ok++;
            } else {
                exceeded++;
            }
        }

        List<String> strengths = new ArrayList<String>();
        List<String> opportunities = new ArrayList<String>();
        if (ok > 0) {
            strengths.add("Estas dentro de " + ok + " de " + scopedBudgets.size() + " presupuestos.");
        }
        if (exceeded > 0) {
            opportunities.add("Tienes " + exceeded + " presupuestos excedidos.");
        }
        int score = exceeded == 0 ? 30 : Math.max(0, 30 - (exceeded * 8));
        return new BudgetResult(score, strengths, opportunities);
    }

    private BigDecimal total(List<FinancialTransaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getTipo() == type)
                .map(FinancialTransaction::getMonto)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    private String labelFor(int score) {
        if (score >= 80) return "Excelente";
        if (score >= 65) return "Buena";
        if (score >= 45) return "En progreso";
        return "Necesita atencion";
    }

    private List<String> list(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static final class BudgetResult {
        private final int score;
        private final List<String> strengths;
        private final List<String> opportunities;

        private BudgetResult(int score, List<String> strengths, List<String> opportunities) {
            this.score = score;
            this.strengths = strengths;
            this.opportunities = opportunities;
        }
    }
}
