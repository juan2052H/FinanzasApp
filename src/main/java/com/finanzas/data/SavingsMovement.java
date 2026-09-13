package com.finanzas.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a single deposit (positive amount) or withdrawal (negative amount)
 * made against a savings goal, so the monthly savings rate can be computed
 * from actual contributions instead of the whole account balance.
 */
public final class SavingsMovement implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDate fecha;
    private final BigDecimal monto;

    public SavingsMovement(LocalDate fecha, BigDecimal monto) {
        this.fecha = fecha == null ? LocalDate.now() : fecha;
        this.monto = monto == null ? BigDecimal.ZERO : monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
