package com.finanzas.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Frequency {
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        YEARLY,
        CUSTOM
    }

    private Transaccion.Tipo tipo;
    private String categoria;
    private String descripcion;
    private BigDecimal montoDecimal;
    private double monto;
    private Frequency frequency;
    private int customIntervalDays;
    private LocalDate nextDate;
    private boolean active;
    private String backendId = "";
    private String backendCategoryId = "";

    public RecurringTransaction(Transaccion.Tipo tipo, String categoria, String descripcion, BigDecimal monto,
                                Frequency frequency, int customIntervalDays, LocalDate nextDate) {
        this.tipo = tipo == null ? Transaccion.Tipo.GASTO : tipo;
        this.categoria = categoria == null ? "Otros" : categoria;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        setMontoDecimal(monto);
        this.frequency = frequency == null ? Frequency.MONTHLY : frequency;
        this.customIntervalDays = Math.max(1, customIntervalDays);
        this.nextDate = nextDate == null ? LocalDate.now() : nextDate;
        this.active = true;
    }

    public Transaccion.Tipo getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return Money.toDouble(getMontoDecimal()); }
    public BigDecimal getMontoDecimal() {
        if (montoDecimal == null) {
            montoDecimal = Money.of(monto);
        }
        return montoDecimal;
    }
    public Frequency getFrequency() { return frequency; }
    public int getCustomIntervalDays() { return customIntervalDays; }
    public LocalDate getNextDate() { return nextDate; }
    public boolean isActive() { return active; }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendCategoryId() { return backendCategoryId == null ? "" : backendCategoryId; }

    public void setMontoDecimal(BigDecimal monto) {
        BigDecimal normalized = Money.normalize(monto);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        this.montoDecimal = normalized;
        this.monto = normalized.doubleValue();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setBackendId(String backendId) {
        this.backendId = backendId == null ? "" : backendId.trim();
    }

    public void setBackendCategoryId(String backendCategoryId) {
        this.backendCategoryId = backendCategoryId == null ? "" : backendCategoryId.trim();
    }

    public Transaccion createTransactionForNextDate() {
        return new Transaccion(tipo, categoria, descripcion, getMontoDecimal(), nextDate);
    }

    public void advanceNextDate() {
        nextDate = calculateFollowingDate(nextDate);
    }

    public LocalDate calculateFollowingDate(LocalDate from) {
        LocalDate base = from == null ? LocalDate.now() : from;
        switch (frequency) {
            case WEEKLY:
                return base.plusWeeks(1);
            case BIWEEKLY:
                return base.plusWeeks(2);
            case YEARLY:
                return base.plusYears(1);
            case CUSTOM:
                return base.plusDays(customIntervalDays);
            case MONTHLY:
            default:
                return base.plusMonths(1);
        }
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        setMontoDecimal(montoDecimal == null ? Money.of(monto) : montoDecimal);
        if (frequency == null) {
            frequency = Frequency.MONTHLY;
        }
        customIntervalDays = Math.max(1, customIntervalDays);
        if (nextDate == null) {
            nextDate = LocalDate.now();
        }
    }
}
