package com.finanzas.data;

import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Money;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Transaccion.Tipo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.math.RoundingMode;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

final class FinancialInsights {
    private FinancialInsights() {
    }

    static double totalIngresos(List<Transaccion> transacciones) {
        return totalByType(transacciones, Tipo.INGRESO);
    }

    static double totalGastos(List<Transaccion> transacciones) {
        return totalByType(transacciones, Tipo.GASTO);
    }

    static double totalAhorros(List<MetaAhorro> metas) {
        return metas.stream()
                .map(MetaAhorro::getMontoActualDecimal)
                .reduce(Money.ZERO, BigDecimal::add)
                .doubleValue();
    }

    static List<Transaccion> buscarTransacciones(List<Transaccion> transacciones, String texto, Tipo tipo) {
        String query = texto == null ? "" : texto.toLowerCase();
        return transacciones.stream()
                .filter(transaccion -> tipo == null || transaccion.getTipo() == tipo)
                .filter(transaccion -> query.isEmpty()
                        || transaccion.getDescripcion().toLowerCase().contains(query)
                        || transaccion.getCategoria().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    static double totalByTypeInMonth(List<Transaccion> transacciones, Tipo tipo, YearMonth month) {
        return transacciones.stream()
                .filter(transaccion -> transaccion.getTipo() == tipo)
                .filter(transaccion -> YearMonth.from(transaccion.getFecha()).equals(month))
                .map(Transaccion::getMontoDecimal)
                .reduce(Money.ZERO, BigDecimal::add)
                .doubleValue();
    }

    static double totalGastosByCategoryInMonth(List<Transaccion> transacciones, String categoria, YearMonth month) {
        String normalizedCategory = normalize(categoria);
        return transacciones.stream()
                .filter(transaccion -> transaccion.getTipo() == Tipo.GASTO)
                .filter(transaccion -> YearMonth.from(transaccion.getFecha()).equals(month))
                .filter(transaccion -> normalize(transaccion.getCategoria()).equals(normalizedCategory))
                .map(Transaccion::getMontoDecimal)
                .reduce(Money.ZERO, BigDecimal::add)
                .doubleValue();
    }

    static double totalByTypeBetween(List<Transaccion> transacciones, Tipo tipo, LocalDate startInclusive, LocalDate endInclusive) {
        return transacciones.stream()
                .filter(transaccion -> transaccion.getTipo() == tipo)
                .filter(transaccion -> !transaccion.getFecha().isBefore(startInclusive) && !transaccion.getFecha().isAfter(endInclusive))
                .map(Transaccion::getMontoDecimal)
                .reduce(Money.ZERO, BigDecimal::add)
                .doubleValue();
    }

    static String[] monthLabels(int months) {
        String[] labels = new String[months];
        YearMonth first = YearMonth.now().minusMonths(months - 1L);
        String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        for (int i = 0; i < months; i++) {
            YearMonth month = first.plusMonths(i);
            labels[i] = monthNames[month.getMonthValue() - 1];
        }
        return labels;
    }

    static double[] totalsByMonth(List<Transaccion> transacciones, Tipo tipo, int months) {
        double[] totals = new double[months];
        YearMonth first = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            totals[i] = totalByTypeInMonth(transacciones, tipo, first.plusMonths(i));
        }
        return totals;
    }

    static Map<String, Double> gastosPorCategoria(List<Transaccion> transacciones) {
        Map<String, BigDecimal> decimalTotals = new LinkedHashMap<String, BigDecimal>();
        transacciones.stream()
                .filter(transaccion -> transaccion.getTipo() == Tipo.GASTO)
                .forEach(transaccion -> decimalTotals.merge(transaccion.getCategoria(), transaccion.getMontoDecimal(), BigDecimal::add));
        return decimalTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Money.toDouble(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    static String[] categoryLabels(List<Transaccion> transacciones) {
        return gastosPorCategoria(transacciones).keySet().toArray(new String[0]);
    }

    static double[] categoryPercentages(List<Transaccion> transacciones) {
        Map<String, Double> totals = gastosPorCategoria(transacciones);
        double total = totals.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return new double[0];
        }
        double[] percentages = new double[totals.size()];
        int index = 0;
        for (double value : totals.values()) {
            percentages[index++] = (value / total) * 100.0d;
        }
        return percentages;
    }

    static String variationVsPreviousMonth(List<Transaccion> transacciones, Tipo tipo) {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        double currentTotal = totalByTypeInMonth(transacciones, tipo, current);
        double previousTotal = totalByTypeInMonth(transacciones, tipo, previous);
        if (previousTotal <= 0) {
            return "Sin periodo anterior para comparar";
        }
        double variation = ((currentTotal - previousTotal) / previousTotal) * 100.0d;
        String sign = variation >= 0 ? "+" : "";
        return sign + String.format(Locale.US, "%.1f", variation) + "% vs mes anterior";
    }

    static List<RecurringExpenseInsight> detectPossibleRecurringExpenses(List<Transaccion> transacciones) {
        Map<String, List<Transaccion>> groups = new HashMap<String, List<Transaccion>>();
        for (Transaccion transaccion : transacciones) {
            if (transaccion.getTipo() != Tipo.GASTO) {
                continue;
            }
            String normalizedDescription = normalize(transaccion.getDescripcion());
            if (normalizedDescription.length() < 3) {
                continue;
            }
            String key = normalize(transaccion.getCategoria()) + "|" + normalizedDescription;
            groups.computeIfAbsent(key, ignored -> new ArrayList<Transaccion>()).add(transaccion);
        }

        List<RecurringExpenseInsight> insights = new ArrayList<RecurringExpenseInsight>();
        for (List<Transaccion> group : groups.values()) {
            long distinctMonths = group.stream()
                    .map(transaccion -> YearMonth.from(transaccion.getFecha()))
                    .distinct()
                    .count();
            if (group.size() < 2 || distinctMonths < 2 || !amountsAreSimilar(group)) {
                continue;
            }
            BigDecimal total = group.stream()
                    .map(Transaccion::getMontoDecimal)
                    .reduce(Money.ZERO, BigDecimal::add);
            BigDecimal average = total.divide(BigDecimal.valueOf(group.size()), Money.SCALE, RoundingMode.HALF_UP);
            Transaccion sample = group.get(0);
            insights.add(new RecurringExpenseInsight(sample.getDescripcion(), sample.getCategoria(), average, group.size()));
        }
        insights.sort((left, right) -> right.getEstimatedMonthlyAmount().compareTo(left.getEstimatedMonthlyAmount()));
        return insights;
    }

    private static double totalByType(List<Transaccion> transacciones, Tipo tipo) {
        return transacciones.stream()
                .filter(transaccion -> transaccion.getTipo() == tipo)
                .map(Transaccion::getMontoDecimal)
                .reduce(Money.ZERO, BigDecimal::add)
                .doubleValue();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean amountsAreSimilar(List<Transaccion> group) {
        BigDecimal total = group.stream()
                .map(Transaccion::getMontoDecimal)
                .reduce(Money.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(group.size()), Money.SCALE, RoundingMode.HALF_UP);
        BigDecimal tolerance = average.multiply(BigDecimal.valueOf(0.10d));
        for (Transaccion transaccion : group) {
            BigDecimal diff = transaccion.getMontoDecimal().subtract(average).abs();
            if (diff.compareTo(tolerance) > 0) {
                return false;
            }
        }
        return true;
    }
}
