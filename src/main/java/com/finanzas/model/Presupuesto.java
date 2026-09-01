package com.finanzas.model;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Presupuesto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String categoria;
    private BigDecimal montoPresupuestadoDecimal;
    private BigDecimal montoGastadoDecimal;
    private double montoPresupuestado;
    private double montoGastado;
    private String color;
    private String backendId = "";
    private String backendCategoryId = "";
    private String backendPeriodMonth = "";

    public Presupuesto(String categoria, double montoPresupuestado, double montoGastado, String color) {
        this(categoria, Money.of(montoPresupuestado), Money.of(montoGastado), color);
    }

    public Presupuesto(String categoria, BigDecimal montoPresupuestado, BigDecimal montoGastado, String color) {
        this.categoria = categoria;
        setMontoPresupuestadoDecimal(montoPresupuestado);
        setMontoGastadoDecimal(montoGastado);
        this.color = color;
    }

    public String getCategoria() { return categoria; }
    public double getMontoPresupuestado() { return Money.toDouble(getMontoPresupuestadoDecimal()); }
    public BigDecimal getMontoPresupuestadoDecimal() {
        if (montoPresupuestadoDecimal == null) {
            montoPresupuestadoDecimal = Money.of(montoPresupuestado);
        }
        return montoPresupuestadoDecimal;
    }
    public double getMontoGastado() { return Money.toDouble(getMontoGastadoDecimal()); }
    public BigDecimal getMontoGastadoDecimal() {
        if (montoGastadoDecimal == null) {
            montoGastadoDecimal = Money.of(montoGastado);
        }
        return montoGastadoDecimal;
    }
    public String getColor() { return color; }
    public double getPorcentajeUsado() {
        if (getMontoPresupuestadoDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return getMontoGastadoDecimal()
                .multiply(BigDecimal.valueOf(100))
                .divide(getMontoPresupuestadoDecimal(), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }
    public double getSaldo() { return Money.toDouble(getSaldoDecimal()); }
    public BigDecimal getSaldoDecimal() { return Money.normalize(getMontoPresupuestadoDecimal().subtract(getMontoGastadoDecimal())); }
    public String getBackendId() { return backendId == null ? "" : backendId; }
    public String getBackendCategoryId() { return backendCategoryId == null ? "" : backendCategoryId; }
    public String getBackendPeriodMonth() { return backendPeriodMonth == null ? "" : backendPeriodMonth; }

    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setMontoPresupuestado(double montoPresupuestado) { setMontoPresupuestadoDecimal(Money.of(montoPresupuestado)); }
    public void setMontoPresupuestadoDecimal(BigDecimal montoPresupuestado) {
        this.montoPresupuestadoDecimal = Money.normalize(montoPresupuestado);
        this.montoPresupuestado = this.montoPresupuestadoDecimal.doubleValue();
    }
    public void setMontoGastado(double montoGastado) { setMontoGastadoDecimal(Money.of(montoGastado)); }
    public void setMontoGastadoDecimal(BigDecimal montoGastado) {
        this.montoGastadoDecimal = Money.normalize(montoGastado);
        this.montoGastado = this.montoGastadoDecimal.doubleValue();
    }
    public void setColor(String color) { this.color = color; }
    public void setBackendId(String backendId) { this.backendId = backendId == null ? "" : backendId.trim(); }
    public void setBackendCategoryId(String backendCategoryId) { this.backendCategoryId = backendCategoryId == null ? "" : backendCategoryId.trim(); }
    public void setBackendPeriodMonth(String backendPeriodMonth) { this.backendPeriodMonth = backendPeriodMonth == null ? "" : backendPeriodMonth.trim(); }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        setMontoPresupuestadoDecimal(montoPresupuestadoDecimal == null ? Money.of(montoPresupuestado) : montoPresupuestadoDecimal);
        setMontoGastadoDecimal(montoGastadoDecimal == null ? Money.of(montoGastado) : montoGastadoDecimal);
    }
}
