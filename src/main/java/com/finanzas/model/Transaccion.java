package com.finanzas.model;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaccion implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo { INGRESO, GASTO }

    private Tipo tipo;
    private String categoria;
    private String descripcion;
    private BigDecimal montoDecimal;
    private double monto;
    private LocalDate fecha;
    private String backendId = "";
    private String backendCategoryId = "";

    public Transaccion(Tipo tipo, String categoria, String descripcion, double monto, LocalDate fecha) {
        this(tipo, categoria, descripcion, Money.of(monto), fecha);
    }

    public Transaccion(Tipo tipo, String categoria, String descripcion, BigDecimal monto, LocalDate fecha) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        setMontoDecimal(monto);
        this.fecha = fecha;
    }

    public Tipo getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return Money.toDouble(getMontoDecimal()); }
    public BigDecimal getMontoDecimal() {
        if (montoDecimal == null) {
            montoDecimal = Money.of(monto);
        }
        return montoDecimal;
    }
    public LocalDate getFecha() { return fecha; }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendCategoryId() { return backendCategoryId == null ? "" : backendCategoryId; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setMonto(double monto) { setMontoDecimal(Money.of(monto)); }
    public void setMontoDecimal(BigDecimal monto) {
        this.montoDecimal = Money.normalize(monto);
        this.monto = this.montoDecimal.doubleValue();
    }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setBackendId(String backendId) { this.backendId = backendId == null ? "" : backendId.trim(); }
    public void setBackendCategoryId(String backendCategoryId) { this.backendCategoryId = backendCategoryId == null ? "" : backendCategoryId.trim(); }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (montoDecimal == null) {
            montoDecimal = Money.of(monto);
        } else {
            montoDecimal = Money.normalize(montoDecimal);
            monto = montoDecimal.doubleValue();
        }
    }
}
