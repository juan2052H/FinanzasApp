package com.finanzas.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GastoHogar implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum SplitMethod {
        EQUAL,
        PERCENTAGE,
        CUSTOM_AMOUNT
    }

    private String descripcion;
    private String categoria;
    private BigDecimal montoDecimal;
    private double monto;
    private String pagadoPor;
    private LocalDate fecha;
    private boolean dividido;
    private SplitMethod splitMethod;
    private Map<String, BigDecimal> splitAmounts;
    private String backendId = "";
    private String backendCategoryId = "";
    private String backendPaidByUserId = "";

    public GastoHogar(String descripcion, String categoria, double monto, String pagadoPor, LocalDate fecha, boolean dividido) {
        this(descripcion, categoria, Money.of(monto), pagadoPor, fecha, dividido);
    }

    public GastoHogar(String descripcion, String categoria, BigDecimal monto, String pagadoPor, LocalDate fecha, boolean dividido) {
        this.descripcion = descripcion;
        this.categoria = categoria;
        setMontoDecimal(monto);
        this.pagadoPor = pagadoPor;
        this.fecha = fecha;
        this.dividido = dividido;
        this.splitMethod = SplitMethod.EQUAL;
        this.splitAmounts = new LinkedHashMap<String, BigDecimal>();
    }

    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getMonto() { return Money.toDouble(getMontoDecimal()); }
    public BigDecimal getMontoDecimal() {
        if (montoDecimal == null) {
            montoDecimal = Money.of(monto);
        }
        return montoDecimal;
    }
    public String getPagadoPor() { return pagadoPor; }
    public LocalDate getFecha() { return fecha; }
    public boolean isDividido() { return dividido; }
    public SplitMethod getSplitMethod() { return splitMethod == null ? SplitMethod.EQUAL : splitMethod; }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendCategoryId() { return backendCategoryId == null ? "" : backendCategoryId; }
    public String getBackendPaidByUserId() { return backendPaidByUserId == null ? "" : backendPaidByUserId; }
    public Map<String, BigDecimal> getSplitAmounts() {
        ensureSplitAmounts();
        return Collections.unmodifiableMap(splitAmounts);
    }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setMonto(double monto) { setMontoDecimal(Money.of(monto)); }
    public void setMontoDecimal(BigDecimal monto) {
        montoDecimal = Money.normalize(monto);
        this.monto = montoDecimal.doubleValue();
    }
    public void setPagadoPor(String pagadoPor) { this.pagadoPor = pagadoPor; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setDividido(boolean dividido) { this.dividido = dividido; }
    public void setBackendId(String backendId) { this.backendId = backendId == null ? "" : backendId.trim(); }
    public void setBackendCategoryId(String backendCategoryId) { this.backendCategoryId = backendCategoryId == null ? "" : backendCategoryId.trim(); }
    public void setBackendPaidByUserId(String backendPaidByUserId) { this.backendPaidByUserId = backendPaidByUserId == null ? "" : backendPaidByUserId.trim(); }

    public void defineEqualSplit(List<String> members) {
        splitMethod = SplitMethod.EQUAL;
        splitAmounts = new LinkedHashMap<String, BigDecimal>();
        if (!dividido || members == null || members.isEmpty()) {
            return;
        }

        BigDecimal share = getMontoDecimal().divide(BigDecimal.valueOf(members.size()), Money.SCALE, Money.ROUNDING);
        BigDecimal assigned = Money.ZERO;
        for (int i = 0; i < members.size(); i++) {
            String member = members.get(i);
            BigDecimal amount = i == members.size() - 1 ? getMontoDecimal().subtract(assigned) : share;
            splitAmounts.put(member, Money.normalize(amount));
            assigned = assigned.add(amount);
        }
    }

    public void definePercentageSplit(Map<String, BigDecimal> percentages) {
        if (percentages == null || percentages.isEmpty()) {
            throw new IllegalArgumentException("Los porcentajes de division son obligatorios.");
        }
        BigDecimal totalPercent = percentages.values().stream().map(Money::normalize).reduce(Money.ZERO, BigDecimal::add);
        if (totalPercent.compareTo(BigDecimal.valueOf(100).setScale(Money.SCALE, Money.ROUNDING)) != 0) {
            throw new IllegalArgumentException("Los porcentajes deben sumar 100.");
        }
        splitMethod = SplitMethod.PERCENTAGE;
        dividido = true;
        splitAmounts = new LinkedHashMap<String, BigDecimal>();
        BigDecimal assigned = Money.ZERO;
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : percentages.entrySet()) {
            BigDecimal amount = index == percentages.size() - 1
                    ? getMontoDecimal().subtract(assigned)
                    : getMontoDecimal().multiply(entry.getValue()).divide(BigDecimal.valueOf(100), Money.SCALE, Money.ROUNDING);
            splitAmounts.put(entry.getKey(), Money.normalize(amount));
            assigned = assigned.add(amount);
            index++;
        }
    }

    public void defineCustomAmountSplit(Map<String, BigDecimal> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            throw new IllegalArgumentException("Los montos de division son obligatorios.");
        }
        BigDecimal total = amounts.values().stream().map(Money::normalize).reduce(Money.ZERO, BigDecimal::add);
        if (total.compareTo(getMontoDecimal()) != 0) {
            throw new IllegalArgumentException("Los montos personalizados deben sumar el total del gasto.");
        }
        splitMethod = SplitMethod.CUSTOM_AMOUNT;
        dividido = true;
        splitAmounts = new LinkedHashMap<String, BigDecimal>();
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            splitAmounts.put(entry.getKey(), Money.normalize(entry.getValue()));
        }
    }

    private void ensureSplitAmounts() {
        if (splitAmounts == null) {
            splitAmounts = new LinkedHashMap<String, BigDecimal>();
        }
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        setMontoDecimal(montoDecimal == null ? Money.of(monto) : montoDecimal);
        if (splitMethod == null) {
            splitMethod = SplitMethod.EQUAL;
        }
        ensureSplitAmounts();
    }
}
