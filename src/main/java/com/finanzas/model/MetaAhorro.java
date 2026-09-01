package com.finanzas.model;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MetaAhorro implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private String icono;
    private BigDecimal montoActualDecimal;
    private BigDecimal montoMetaDecimal;
    private double montoActual;
    private double montoMeta;
    private int progreso;
    private String color;
    private LocalDate fechaLimite;
    private String backendId = "";
    private String backendWorkspaceId = "";

    public MetaAhorro(String nombre, String icono, double montoActual, double montoMeta, String color) {
        this(nombre, icono, montoActual, montoMeta, color, LocalDate.now().plusMonths(6));
    }

    public MetaAhorro(String nombre, String icono, double montoActual, double montoMeta, String color, LocalDate fechaLimite) {
        this(nombre, icono, Money.of(montoActual), Money.of(montoMeta), color, fechaLimite);
    }

    public MetaAhorro(String nombre, String icono, BigDecimal montoActual, BigDecimal montoMeta, String color, LocalDate fechaLimite) {
        this.nombre = nombre;
        this.icono = icono;
        setMontoActualDecimal(montoActual);
        setMontoMetaDecimal(montoMeta);
        this.color = color;
        this.fechaLimite = fechaLimite;
        recalculateProgress();
    }

    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public double getMontoActual() { return Money.toDouble(getMontoActualDecimal()); }
    public BigDecimal getMontoActualDecimal() {
        if (montoActualDecimal == null) {
            montoActualDecimal = Money.of(montoActual);
        }
        return montoActualDecimal;
    }
    public double getMontoMeta() { return Money.toDouble(getMontoMetaDecimal()); }
    public BigDecimal getMontoMetaDecimal() {
        if (montoMetaDecimal == null) {
            montoMetaDecimal = Money.of(montoMeta);
        }
        return montoMetaDecimal;
    }
    public int getProgreso() { return progreso; }
    public String getColor() { return color; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendWorkspaceId() { return backendWorkspaceId == null ? "" : backendWorkspaceId; }
    public double getFaltante() { return Money.toDouble(getFaltanteDecimal()); }
    public BigDecimal getFaltanteDecimal() {
        BigDecimal faltante = getMontoMetaDecimal().subtract(getMontoActualDecimal());
        return faltante.compareTo(BigDecimal.ZERO) < 0 ? Money.ZERO : Money.normalize(faltante);
    }
    public long getDiasRestantes() { return ChronoUnit.DAYS.between(LocalDate.now(), fechaLimite); }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setIcono(String icono) { this.icono = icono; }
    public void setMontoActual(double montoActual) {
        setMontoActualDecimal(Money.of(montoActual));
    }
    public void setMontoActualDecimal(BigDecimal montoActual) {
        this.montoActualDecimal = Money.normalize(montoActual);
        this.montoActual = this.montoActualDecimal.doubleValue();
        recalculateProgress();
    }
    public void setMontoMeta(double montoMeta) {
        setMontoMetaDecimal(Money.of(montoMeta));
    }
    public void setMontoMetaDecimal(BigDecimal montoMeta) {
        this.montoMetaDecimal = Money.normalize(montoMeta);
        this.montoMeta = this.montoMetaDecimal.doubleValue();
        recalculateProgress();
    }
    public void setColor(String color) { this.color = color; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public void setBackendId(String backendId) { this.backendId = backendId == null ? "" : backendId.trim(); }
    public void setBackendWorkspaceId(String backendWorkspaceId) { this.backendWorkspaceId = backendWorkspaceId == null ? "" : backendWorkspaceId.trim(); }

    private void recalculateProgress() {
        if (getMontoMetaDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            progreso = 0;
            return;
        }
        BigDecimal percent = getMontoActualDecimal()
                .multiply(BigDecimal.valueOf(100))
                .divide(getMontoMetaDecimal(), 0, Money.ROUNDING);
        progreso = Math.min(100, percent.intValue());
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        montoActualDecimal = montoActualDecimal == null ? Money.of(montoActual) : Money.normalize(montoActualDecimal);
        montoMetaDecimal = montoMetaDecimal == null ? Money.of(montoMeta) : Money.normalize(montoMetaDecimal);
        montoActual = montoActualDecimal.doubleValue();
        montoMeta = montoMetaDecimal.doubleValue();
        recalculateProgress();
    }
}
