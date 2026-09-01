package com.finanzas.api;

import java.math.BigDecimal;

public final class BackendSavingsSummary {
    private final BigDecimal ingresos;
    private final BigDecimal gastos;
    private final BigDecimal ahorroTotal;
    private final BigDecimal ahorroAsignadoAMetas;
    private final BigDecimal ahorroLibre;
    private final BigDecimal retirosAcumulados;
    private final BigDecimal saldoDisponibleNoAhorrado;
    private final BigDecimal tasaAhorro;

    BackendSavingsSummary(BigDecimal ingresos, BigDecimal gastos, BigDecimal ahorroTotal,
                          BigDecimal ahorroAsignadoAMetas, BigDecimal ahorroLibre,
                          BigDecimal retirosAcumulados, BigDecimal saldoDisponibleNoAhorrado,
                          BigDecimal tasaAhorro) {
        this.ingresos = value(ingresos);
        this.gastos = value(gastos);
        this.ahorroTotal = value(ahorroTotal);
        this.ahorroAsignadoAMetas = value(ahorroAsignadoAMetas);
        this.ahorroLibre = value(ahorroLibre);
        this.retirosAcumulados = value(retirosAcumulados);
        this.saldoDisponibleNoAhorrado = value(saldoDisponibleNoAhorrado);
        this.tasaAhorro = value(tasaAhorro);
    }

    public BigDecimal getIngresos() { return ingresos; }
    public BigDecimal getGastos() { return gastos; }
    public BigDecimal getAhorroTotal() { return ahorroTotal; }
    public BigDecimal getAhorroAsignadoAMetas() { return ahorroAsignadoAMetas; }
    public BigDecimal getAhorroLibre() { return ahorroLibre; }
    public BigDecimal getRetirosAcumulados() { return retirosAcumulados; }
    public BigDecimal getSaldoDisponibleNoAhorrado() { return saldoDisponibleNoAhorrado; }
    public BigDecimal getTasaAhorro() { return tasaAhorro; }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
