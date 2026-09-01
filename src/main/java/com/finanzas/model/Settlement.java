package com.finanzas.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Settlement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fromMember;
    private String toMember;
    private BigDecimal amountDecimal;
    private double amount;
    private LocalDate date;
    private String note;
    private String backendId = "";
    private String backendFromUserId = "";
    private String backendToUserId = "";

    public Settlement(String fromMember, String toMember, BigDecimal amount, LocalDate date, String note) {
        if (fromMember == null || fromMember.trim().isEmpty() || toMember == null || toMember.trim().isEmpty()) {
            throw new IllegalArgumentException("Los miembros de la liquidacion son obligatorios.");
        }
        if (fromMember.equals(toMember)) {
            throw new IllegalArgumentException("La liquidacion requiere dos miembros distintos.");
        }
        this.fromMember = fromMember.trim();
        this.toMember = toMember.trim();
        setAmountDecimal(amount);
        this.date = date == null ? LocalDate.now() : date;
        this.note = note == null ? "" : note.trim();
    }

    public String getFromMember() { return fromMember; }
    public String getToMember() { return toMember; }
    public double getAmount() { return Money.toDouble(getAmountDecimal()); }
    public BigDecimal getAmountDecimal() {
        if (amountDecimal == null) {
            amountDecimal = Money.of(amount);
        }
        return amountDecimal;
    }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendFromUserId() { return backendFromUserId == null ? "" : backendFromUserId; }
    public String getBackendToUserId() { return backendToUserId == null ? "" : backendToUserId; }

    public void setAmountDecimal(BigDecimal amount) {
        BigDecimal normalized = Money.normalize(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de liquidacion debe ser mayor a cero.");
        }
        amountDecimal = normalized;
        this.amount = normalized.doubleValue();
    }

    public void setBackendId(String backendId) { this.backendId = backendId == null ? "" : backendId.trim(); }
    public void setBackendFromUserId(String backendFromUserId) { this.backendFromUserId = backendFromUserId == null ? "" : backendFromUserId.trim(); }
    public void setBackendToUserId(String backendToUserId) { this.backendToUserId = backendToUserId == null ? "" : backendToUserId.trim(); }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        setAmountDecimal(amountDecimal == null ? Money.of(amount) : amountDecimal);
        if (date == null) {
            date = LocalDate.now();
        }
        if (note == null) {
            note = "";
        }
    }
}
