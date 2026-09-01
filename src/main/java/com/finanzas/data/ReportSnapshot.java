package com.finanzas.data;

import com.finanzas.model.Money;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Transaccion.Tipo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportSnapshot {
    private static final String[] MONTH_NAMES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final ReportPeriod period;
    private final List<Transaccion> transactions;
    private final BigDecimal income;
    private final BigDecimal expenses;
    private final BigDecimal balance;
    private final BigDecimal savings;
    private final BigDecimal savingsRatePercent;
    private final Map<String, BigDecimal> expenseByCategory;

    public ReportSnapshot(ReportPeriod period, List<Transaccion> transactions, BigDecimal income,
                          BigDecimal expenses, Map<String, BigDecimal> expenseByCategory) {
        this.period = period;
        this.transactions = Collections.unmodifiableList(new ArrayList<Transaccion>(transactions));
        this.income = Money.normalize(income);
        this.expenses = Money.normalize(expenses);
        this.balance = Money.normalize(this.income.subtract(this.expenses));
        this.savings = this.balance.compareTo(BigDecimal.ZERO) > 0 ? this.balance : Money.ZERO;
        this.savingsRatePercent = this.income.compareTo(BigDecimal.ZERO) <= 0
                ? Money.ZERO
                : this.savings.multiply(BigDecimal.valueOf(100)).divide(this.income, 2, RoundingMode.HALF_UP);
        this.expenseByCategory = Collections.unmodifiableMap(new LinkedHashMap<String, BigDecimal>(expenseByCategory));
    }

    public ReportPeriod getPeriod() {
        return period;
    }

    public List<Transaccion> getTransactions() {
        return transactions;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getSavings() {
        return savings;
    }

    public BigDecimal getSavingsRatePercent() {
        return savingsRatePercent;
    }

    public Map<String, BigDecimal> getExpenseByCategory() {
        return expenseByCategory;
    }

    public boolean hasTransactions() {
        return !transactions.isEmpty();
    }

    public String[] getCategoryLabels() {
        return expenseByCategory.keySet().toArray(new String[0]);
    }

    public double[] getCategoryPercentages() {
        if (expenses.compareTo(BigDecimal.ZERO) <= 0 || expenseByCategory.isEmpty()) {
            return new double[0];
        }
        double[] percentages = new double[expenseByCategory.size()];
        int index = 0;
        for (BigDecimal value : expenseByCategory.values()) {
            percentages[index++] = value.multiply(BigDecimal.valueOf(100))
                    .divide(expenses, 4, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return percentages;
    }

    public String[] getMonthLabels() {
        List<YearMonth> months = visibleMonths();
        String[] labels = new String[months.size()];
        for (int i = 0; i < months.size(); i++) {
            YearMonth month = months.get(i);
            labels[i] = MONTH_NAMES[month.getMonthValue() - 1] + " " + String.valueOf(month.getYear()).substring(2);
        }
        return labels;
    }

    public double[] getMonthlyIncomeTotals() {
        return monthlyTotals(Tipo.INGRESO);
    }

    public double[] getMonthlyExpenseTotals() {
        return monthlyTotals(Tipo.GASTO);
    }

    private double[] monthlyTotals(Tipo tipo) {
        List<YearMonth> months = visibleMonths();
        double[] totals = new double[months.size()];
        for (int i = 0; i < months.size(); i++) {
            BigDecimal total = Money.ZERO;
            YearMonth month = months.get(i);
            for (Transaccion transaction : transactions) {
                if (transaction.getTipo() == tipo && YearMonth.from(transaction.getFecha()).equals(month)) {
                    total = total.add(transaction.getMontoDecimal());
                }
            }
            totals[i] = Money.toDouble(total);
        }
        return totals;
    }

    private List<YearMonth> visibleMonths() {
        LocalDate start = period.getStartDate();
        LocalDate end = period.getEndDate();
        for (Transaccion transaction : transactions) {
            LocalDate date = transaction.getFecha();
            if (start == null || date.isBefore(start)) {
                start = date;
            }
            if (end == null || date.isAfter(end)) {
                end = date;
            }
        }
        if (end == null) {
            end = LocalDate.now();
        }
        if (start == null) {
            start = end.withDayOfMonth(1);
        }

        YearMonth first = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        List<YearMonth> months = new ArrayList<YearMonth>();
        YearMonth cursor = first;
        while (!cursor.isAfter(last)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        while (months.size() > 12) {
            months.remove(0);
        }
        return months;
    }
}
